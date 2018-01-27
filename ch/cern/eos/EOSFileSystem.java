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

/*from java.io import ByteArrayInputStream, ByteArrayOutputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream*/

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


//import ch.cern.eos.EOSInputStream;

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

//    private native static void setcc(String ccname);
    public native String getErrText(long errcode);

    private static final Log LOG = LogFactory.getLog(EOSFileSystem.class);
    private URI uri;

    public static boolean EOS_debug = false;
    public static int buffer_size = 32*1024*1024;

    public boolean kerberos = true;


    public EOSFileSystem() {
    }

    public URI toUri(Path p) throws IOException {
	try {
	    if (p.getName().indexOf('?') < 0)
		return p.toUri();
	    URI u = new URI(p.toString());	    // need to re-parse otherwise '?query' becomes part of filename
	    if (EOS_debug) System.out.println("EOSFileSystem.toUri (Scheme,Authority,Path,Query): " + u.getScheme() + "," + u.getAuthority() + "," + u.getPath() + "," + u.getQuery());
	    return u;
	} catch (URISyntaxException e) {
	    if (EOS_debug) e.printStackTrace();
	    throw new IOException("Invalid URI Syntax");
	}
    }

     public String toFilePath(Path p) throws IOException {
	URI u = p.toUri();
	String s = u.getPath();

	if (u.getQuery() != null)  s += "?" + u.getQuery();
	return s;
    }


//    public int FileStatus(long length, boolean isdir, int block_replication,
//	                         long blocksize, long modification_time, Path path) { int xx=42; return xx;}

    public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) {
	throw new IllegalArgumentException("append");
    }

    public FSDataOutputStream create(Path p, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
	if (nHandle == 0) initHandle();
	String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + toFilePath(p);

	if (EOS_debug) {
		System.out.println("EOSfs create " + filespec);
	}
	return new FSDataOutputStream(new EOSOutputStream(filespec, permission, overwrite), null);
    }

    public boolean delete(Path p, boolean recursive) throws IOException {
	if (nHandle == 0) initHandle();
	
	long status = 0;
	String filespec = toUri(p).getPath();

	FileStatus std = getFileStatusS(nHandle, filespec, p);

	if (std.isDirectory()) {
	    if (recursive) {
		if (EOS_debug) System.out.println("EOSFileSystem.delete recursive " + filespec);
		FileStatus st[] = listStatus(p);
		for (FileStatus s: st) {
		    if (s.isDirectory()) {
			if (!delete(s.getPath(), recursive)) break;
		    } else { 
			status = Rm(nHandle, s.getPath().toUri().getPath());
			if (EOS_debug) System.out.println("EOSFileSystem.delete " + s.getPath().toString() + " status = " + status);
			if (status != 0) break;
		    }
		}
	    }
	    if (status == 0) {
		status = RmDir(nHandle, filespec);
		if (EOS_debug) System.out.println("EOSFileSystem.delete RmDir " + filespec + " status = " + status);
	    }
	} else {
	    status = Rm(nHandle, filespec);
	    if (EOS_debug) System.out.println("EOSFileSystem.delete " + filespec + " status = " + status);
	}
	if (status != 0) throw new IOException("Cannot delete " + p.toString() + ", status = " + status);

	return status == 0;
    }

    public static void initLib() throws IOException {
	if (System.getenv("EOS_debug") != null) {
	    EOS_debug = true;
	    EOSKrb5.setDebug(EOS_debug);
	}
	if (libLoaded) return;

	String jlp = System.getProperty("java.library.path");
	if (!jlp.contains("/usr/lib/hadoop/lib/native")) {
	    System.setProperty("java.library.path", "/usr/lib/hadoop/lib/native:" + jlp);
	    if(EOS_debug) System.out.println("EOSfs.initlib: using java.library.path: " + System.getProperty("java.library.path"));
	    //found by googling... not that I understood it...
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

	try {
	    System.loadLibrary("jXrdCl");
	} catch (UnsatisfiedLinkError e) {
	    e.printStackTrace();
	    System.out.println("failed to load jXrdCl, java.library.path=" + jlp + ", 'root' scheme disabled");		/* very likely only says "/usr/lib/hadoop/lib/native" */
	    throw new IOException();
	}

    }


    private void initHandle() throws IOException {
	if (nHandle != 0) return;
	initLib(); 

	String fsStr = uri.getScheme() + "://" + uri.getAuthority();
	nHandle = initFileSystem(uri.getScheme() + "://" + uri.getAuthority());
	if (EOS_debug) System.out.println("initFileSystem(" + fsStr + ") = " + nHandle);

        if(kerberos)
 	   setkrbcc(EOSKrb5.setKrb()); //probably not needed here
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

	// System.out.println("EOS initialize: EOS_debug " + EOS_debug + " uri scheme " + uri.getScheme() + " authority " + uri.getAuthority() + " path " + uri.getPath() + " query " + uri.getQuery());

	this.uri = uri;

        initLib();

        if(kerberos)
	    setkrbcc(EOSKrb5.setKrb());
    }

    public String getScheme() {
	return "root";
    }

    public static URI getDefaultURI(Configuration conf) {
	return URI.create("root://");
    }

    public FSDataInputStream open(Path path, int bufSize) throws IOException {
	if (nHandle == 0) initHandle();
	URI u = toUri(path);
	String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
	// String filespec = u.getPath();
	if (EOS_debug) {
	    System.out.println("EOSfs open " + u.getScheme() + "://" + u.getAuthority() + u.getPath() + " --> " + filespec);
	}
	return new FSDataInputStream(new BufferedFSInputStream (new EOSInputStream(filespec),buffer_size));
    }


    public FileStatus getFileStatus(Path p) throws IOException {
	if (nHandle == 0) initHandle();

	FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
	if (st == null) {
	    throw new FileNotFoundException("File not found");
	}

	if (EOS_debug) System.out.println(st.toString());

	return st;
    }

    public FileStatus[] listStatus(Path p) throws IOException {
	if (nHandle == 0) initHandle();
        FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
        if (st.isFile())
	    return new FileStatus[]{st};
      
	return listFileStatusS(nHandle, toFilePath(p), p);
    }

    public boolean mkdirs(Path p, FsPermission permission) throws IOException {
	if (nHandle == 0) initHandle();
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
	if (st != 0 && EOS_debug) System.out.println("prepare failed on " + uris[0] + "... (" + uris.length + " elements) st = " + st);
	return st == 0;
    }

    public boolean prepare(Path pp[], int pFlags) throws IOException {
	String[] uris = new String[pp.length];

	URI u;

	for (int i = 0; i < pp.length; i++) {
	    uris[i] = toFilePath(pp[i]);
	}

	return prepare(uris, pFlags);
    }


    public boolean rename(Path src, Path dst) throws IOException {
	if (nHandle == 0) initHandle();
	long st = Mv(nHandle, toUri(src).getPath(), toUri(dst).getPath());
	return st == 0;
    };

};

