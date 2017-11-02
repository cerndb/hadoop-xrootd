/* FileSystem wrapper for EOS and Castor "xrootd" based file systems
 *
 * Author: Rainer Toebbicke, CERN IT
 */

package ch.cern.eos;

import java.lang.System;
import java.lang.reflect.Field;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import java.util.Arrays;

import java.lang.UnsatisfiedLinkError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.BufferedFSInputStream;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Progressable;

public class EOSFileSystem extends FileSystem {
    private long nHandle = 0;
    private static boolean libLoaded = false;
    private native long initFileSystem(String url);
    private native FileStatus getFileStatusS(long nHandle, String fn, Path p);
    private native FileStatus[] listFileStatusS(long nHandle, String fn, Path p);
    private native long Mv(long nHandle, String src, String dest);
    private native long Rm(long nHandle, String fn);
    private native long MkDir(long nHandle, String fn, short mode);
    private native long  RmDir(long nHandle, String fn);
    private native long Prepare(long nHandle, String[] uris, int pFlags);
    public static native void setenv(String envname, String envvalue);
    public static native String getenv(String envname);
    public native String getErrText(long errcode);

    private static final Log LOG = LogFactory.getLog(EOSFileSystem.class);
    private URI uri;

	private static EOSDebugLogger eosDebugLogger;
    public static int buffer_size = 32*1024*1024;
	private static final String JAVA_LIB_PATH = "java.library.path";
	private static final String HADOOP_NATIVE_PATH = "/usr/lib/hadoop/lib/native";

    public boolean kerberos = true;

    public EOSFileSystem() {
    }

    public URI toUri(Path p) throws IOException {
		try {
			// if it doesn't have any parameters, return as is
			if (p.getName().indexOf('?') < 0) {
				return p.toUri();
			}
			
			// need to re-parse otherwise '?query' becomes part of filename
			URI u = new URI(p.toString()); 
			eosDebugLogger.print("EOSFileSystem.toUri (Scheme,Authority,Path,Query): " + u.getScheme() + ","
						+ u.getAuthority() + "," + u.getPath() + "," + u.getQuery());
			return u;
		} catch (URISyntaxException e) {
			eosDebugLogger.printStackTrace(e);
			throw new IOException("Invalid URI Syntax");
		}
    }

    public String toFilePath(Path p) throws IOException {
		URI u = p.toUri();
		String s = u.getPath();

		if (u.getQuery() != null) {
			s += "?" + u.getQuery();
		}
		return s;
    }

	public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) {
		throw new IllegalArgumentException("append");
    }

    public FSDataOutputStream create(Path p, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
		initHandle();
		String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + toFilePath(p);
		eosDebugLogger.print("EOSfs create " + filespec);
		return new FSDataOutputStream(new EOSOutputStream(filespec, permission, overwrite), null);
    }

    public boolean delete(Path p, boolean recursive) throws IOException {
		initHandle();
		
		long status = 0;
		String filespec = toUri(p).getPath();

		FileStatus std = getFileStatusS(nHandle, filespec, p);

		if (std.isDirectory()) {
			if (recursive) {
				eosDebugLogger.print("EOSFileSystem.delete recursive " + filespec);
				status = this.deleteRecursiveDirectory(p, status);				
			}

			if (status == 0) {
				status = RmDir(nHandle, filespec);
				eosDebugLogger.print("EOSFileSystem.delete RmDir " + filespec + " status = " + status);
			}
		} else {
			status = Rm(nHandle, filespec);
			eosDebugLogger.print("EOSFileSystem.delete " + filespec + " status = " + status);
		}
		if (status != 0) {
			throw new IOException("Cannot delete " + p.toString() + ", status = " + status);
		} 

		return status == 0;
	}
	
	private long deleteRecursiveDirectory(Path p, long status) throws IOException {
		FileStatus st[] = listStatus(p);
		for (FileStatus s: st) {
			if (s.isDirectory()) {
				if (!delete(s.getPath(), /*recursive*/true)) {
					break;
				}
			} else { 
				status = Rm(nHandle, s.getPath().toUri().getPath());
				eosDebugLogger.print("EOSFileSystem.delete " + s.getPath().toString() + " status = " + status);
				if (status != 0) {
					break;
				}
			}
		}
		return status;
	}

    public static void initLib() throws IOException {
		eosDebugLogger = new EOSDebugLogger(System.getenv("EOS_debug") != null);	
			
		if (libLoaded) {
			return;
		}
		initHadoopNative();
		initJXrdCl();
    }
    
	private static void initHadoopNative() throws IOException {
		String jlp = System.getProperty(JAVA_LIB_PATH);
		if (!jlp.contains(HADOOP_NATIVE_PATH)) {
			System.setProperty(JAVA_LIB_PATH, HADOOP_NATIVE_PATH + ":" + jlp);
			eosDebugLogger.print("EOSfs.initlib: using java.library.path: " + System.getProperty("java.library.path"));
			
			//set sys_paths to null so that java.library.path will be reevaluated next time it is needed
			try {
				final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
				sysPathsField.setAccessible(true);
				sysPathsField.set(null, null);
			} catch (Exception e) {
				System.out.println("Could not reset java.library.path: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private static void initJXrdCl() throws IOException {
		try {
			System.loadLibrary("jXrdCl");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			System.out.println("failed to load jXrdCl");
			throw new IOException();
		}
	}

    private void initHandle() throws IOException {
		if (nHandle != 0) {
			return;
		}

		initLib();

		String fileSystemURI = this.uri.getScheme() + "://" + this.uri.getAuthority();
		this.nHandle = initFileSystem(fileSystemURI);
		eosDebugLogger.print("initFileSystem(" + fileSystemURI + ") = " + nHandle);

        if (kerberos) {
			setkrbcc(EOSKrb5.setKrb()); //probably not needed here
		}
    }
   
    /*setting token cache from TGT (on Spark or MR drivers) or init local krb cache from token (if mapper or executor) */
    public static void setKrb()
    {
		EOSKrb5.setKrb();
    }

    /* This sets (setenv()) KRB5CCNAME in the current (!) environment, which is NOT the one java currently sees, nor the one a java sub-process is going to see spawned
     * using execve() - for the latter one would have to modify java's copy of the environment which is doable.
     * jython or scala may yet play different games
     */
    public static void setkrbcc(String ccname) throws IOException {
		initLib();
		setenv("KRB5CCNAME", "FILE:" + ccname);
    }

    public void initialize(URI uri, Configuration conf) throws IOException {
		setConf(conf);

		this.uri = uri;
        initLib();

        if (kerberos) {
			setkrbcc(EOSKrb5.setKrb());
		}
    }

    public String getScheme() {
		return "root";
    }

    public static URI getDefaultURI(Configuration conf) {
		return URI.create("root://");
    }

    public FSDataInputStream open(Path path, int bufSize) throws IOException {
		initHandle();
		URI u = toUri(path);
		String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
		
		eosDebugLogger.print("EOSfs open " + u.getScheme() + "://" + u.getAuthority() + u.getPath() + " --> " + filespec);
		return new FSDataInputStream(new BufferedFSInputStream (new EOSInputStream(filespec),buffer_size));
    }

    public FileStatus getFileStatus(Path p) throws IOException {
		initHandle();

		FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
		if (st == null) {
			throw new FileNotFoundException("File not found");
		}
		else {
			eosDebugLogger.print(st.toString());
		}
		return st;
    }

    public FileStatus[] listStatus(Path p) throws IOException {
		initHandle();
		return listFileStatusS(nHandle, toFilePath(p), p);
    }

    public boolean mkdirs(Path p, FsPermission permission) throws IOException {
		initHandle();

		String filespec = toFilePath(p);
		long st = MkDir(nHandle, filespec, permission.toShort());
		return st == 0;
    }

    public URI getUri() {
		return uri;
    }

    public Path getWorkingDirectory() {
		return new Path(uri);
    }

    public void setWorkingDirectory(Path f) {
		throw new IllegalArgumentException("setWorkingDirectory");
    }

    public boolean prepare(String[] uris, int pFlags) throws IOException {
		initHandle();
		long st = Prepare(nHandle, uris, pFlags);
		if (st != 0) {
			eosDebugLogger.print("prepare failed on " + uris[0] + "... (" + uris.length + " elements) st = " + st);
		}
		return st == 0;
    }

    public boolean prepare(Path pp[], int pFlags) throws IOException {
		String[] uris = new String[pp.length];
		for (int i = 0; i < pp.length; i++) {
			uris[i] = toFilePath(pp[i]);
		}
		return prepare(uris, pFlags);
    }

    public boolean rename(Path src, Path dst) throws IOException {
		initHandle();
		long st = Mv(nHandle, toUri(src).getPath(), toUri(dst).getPath());
		return st == 0;
    };
};