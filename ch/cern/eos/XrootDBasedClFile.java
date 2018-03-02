/*
    Wrapper class for C++ XrdClFile
*/

package ch.cern.eos;

import java.lang.Throwable;
import java.lang.System;

public class XrootDBasedClFile {
    private long nHandle;
    private native long initFile();
    private native long disposeFile(long handle);
    private native long openFile(long handle, String url, int flags, int mode);
    private native long readFile(long handle, long pos, byte buffer[], int off, int len);
    private native long writeFile(long handle, long pos, byte buffer[], int off, int len);
    private native long syncFile(long handle);
    private native long closeFile(long handle);

    public XrootDBasedClFile() {
        nHandle = initFile();
    }

    public void dispose() {
		try {
		    nHandle = disposeFile(nHandle);
		} catch (Throwable e) {
		    System.err.println("'dispose' failed to delete native object " + nHandle);
		    e.printStackTrace();
		}
    }
	
    protected void finalize() throws Throwable {
		try { 
		    dispose();
		} finally {
		    super.finalize();
		}
    }
	
    public long Open(String url, int flags, int mode) {
		long code = openFile(nHandle, url, flags, mode);
		return code;
    }

    /* read from position filepos up to len bytes into buffer starting at offset off */
    public long Read(long filepos, byte[] buff, int off, int len) {
    	return readFile(nHandle, filepos, buff, off, len);
    }

    /* write from position filepos up to len bytes from buffer starting at offset off */
    public long Write(long filepos, byte[] buff, int off, int len) {
    	return writeFile(nHandle, filepos, buff, off, len);
    }

    public long Close() {
    	return closeFile(nHandle);
    }

    public long Sync() {
    	return syncFile(nHandle);
    }
}

