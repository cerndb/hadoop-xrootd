/*
 * Copyright 2014-2022 CERN IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.cern.eos;

import ch.cern.eos.NarSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class XRootDFileSystem extends FileSystem {

    private static DebugLogger eosDebugLogger;

    static {
        NarSystem.loadLibrary();
    }

    private long nHandle = 0;
    private URI uri;
    private int readAhead;

    public static native void setenv(String envname, String envvalue);

    public static native String getenv(String envname);

    public static URI getDefaultURI(Configuration conf) {
        return URI.create("root://");
    }

    private native long initFileSystem(String url);

    private native FileStatus getFileStatusS(long nHandle, String fn, Path p);

    private native FileStatus[] listFileStatusS(long nHandle, String fn, Path p);

    private native long Mv(long nHandle, String src, String dest);

    private native long Rm(long nHandle, String fn);

    private native long MkDir(long nHandle, String fn, short mode);

    private native long RmDir(long nHandle, String fn);

    private native long Prepare(long nHandle, String[] uris, int pFlags);

    public native String getErrText(long errcode);

    public void initialize(URI uri, Configuration conf) throws IOException {
        super.initialize(uri, conf);
        setConf(conf);

        eosDebugLogger = new DebugLogger(System.getenv("EOS_debug") != null);
        XRootDKrb5.setDebug(eosDebugLogger.isDebugEnabled());
        this.uri = uri;
        this.readAhead = XRootDUtils.byteConfOption(conf, XRootDConstants.READAHEAD_RANGE,
                XRootDConstants.DEFAULT_READAHEAD_RANGE);
        if (this.readAhead == XRootDConstants.DEFAULT_READAHEAD_RANGE) {
            eosDebugLogger.printWarn("Hadoop Config " + XRootDConstants.READAHEAD_RANGE +
                    " not set, using default value " + XRootDConstants.DEFAULT_READAHEAD_RANGE);
        }
    }

    public URI toUri(Path p) throws IOException {
        try {
            // if it doesn't have any parameters, return as is
            if (p.getName().indexOf('?') < 0) {
                return p.toUri();
            }

            // need to re-parse otherwise '?query' becomes part of filename
            URI u = new URI(p.toString());
            eosDebugLogger.printDebug("EOSFileSystem.toUri (Scheme,Authority,Path,Query): " + u.getScheme() + "," + u.getAuthority() + "," + u.getPath() + "," + u.getQuery());
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
        String filespec = uri.getScheme() + "://" + uri.getAuthority() + "/" + toFilePath(p);
        eosDebugLogger.printDebug("EOSfs create " + filespec);
        return new FSDataOutputStream(new XRootDOutputStream(filespec, permission, overwrite), null);
    }

    public boolean delete(Path p, boolean recursive) throws IOException {
        initHandle();

        long status = 0;
        String filespec = toUri(p).getPath();

        FileStatus std = getFileStatusS(nHandle, filespec, p);

        if (std.isDirectory()) {
            if (recursive) {
                eosDebugLogger.printDebug("EOSFileSystem.delete recursive " + filespec);
                status = this.deleteRecursiveDirectory(p, status);
            }

            if (status == 0) {
                status = RmDir(nHandle, filespec);
                eosDebugLogger.printDebug("EOSFileSystem.delete RmDir " + filespec + " status = " + status);
            }
        } else {
            status = Rm(nHandle, filespec);
            eosDebugLogger.printDebug("EOSFileSystem.delete " + filespec + " status = " + status);
        }
        if (status != 0) {
            throw new IOException("Cannot delete " + p.toString() + ", status = " + status);
        }

        return status == 0;
    }

    private long deleteRecursiveDirectory(Path p, long status) throws IOException {
        FileStatus st[] = listStatus(p);
        for (FileStatus s : st) {
            if (s.isDirectory()) {
                if (!delete(s.getPath(), /*recursive*/true)) {
                    break;
                }
            } else {
                status = Rm(nHandle, s.getPath().toUri().getPath());
                eosDebugLogger.printDebug("EOSFileSystem.delete " + s.getPath().toString() + " status = " + status);
                if (status != 0) {
                    break;
                }
            }
        }
        return status;
    }

    protected void initHandle() throws IOException {
        if (nHandle != 0) {
            return;
        }

        String fileSystemURI = this.uri.getScheme() + "://" + this.uri.getAuthority();
        this.nHandle = initFileSystem(fileSystemURI);
        eosDebugLogger.printDebug("initFileSystem(" + fileSystemURI + ") = " + nHandle);
    }

    public String getScheme() {
        return "root";
    }

    public FSDataInputStream open(Path path, int bufSize) throws IOException {
        initHandle();
        URI u = toUri(path);
        String filespec = uri.getScheme() + "://" + uri.getAuthority() + "/" + u.getPath();

        // create object for extra instrumentation of metrics
        XRootDInstrumentation instrumentation = new XRootDInstrumentation();

        // ReadAhead is done with BufferedFSInputStream
        eosDebugLogger.printDebug("EOSfs open " + filespec + " with readAhead=" + readAhead);

        return new FSDataInputStream(new BufferedFSInputStream(new XRootDInputStream(filespec, statistics, instrumentation), readAhead));
    }

    public FileStatus getFileStatus(Path p) throws IOException {
        initHandle();

        FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
        if (st == null) {
            throw new FileNotFoundException("File not found");
        } else {
            eosDebugLogger.printDebug(st.toString());
        }
        return st;
    }

    public FileStatus[] listStatus(Path p) throws IOException {
        initHandle();

        // If isFile return FileStatus directly
        FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
        if (st.isFile())
            return new FileStatus[]{st};

        // If isDirectory use listFileStatusS of path
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
            eosDebugLogger.printDebug("prepare failed on " + uris[0] + "... (" + uris.length + " elements) st = " + st);
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
    }

    ;
};
