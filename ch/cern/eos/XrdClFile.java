/*
    Wrapper class for C++ XrdClFile

    JNI interface to xrootd, used for 
*/

package ch.cern.eos;

import java.lang.Throwable;
import java.lang.System;

public class XrdClFile {
    private long nHandle;
    private native long initFile();
    private native long disposeFile(long handle);
    private native long openFile(long handle, String url, int flags, int mode);
    private native long readFile(long handle, long pos, byte buffer[], int off, int len);
    private native long writeFile(long handle, long pos, byte buffer[], int off, int len);
    private native long syncFile(long handle);
    private native long closeFile(long handle);

    public XrdClFile() {	/* Constructor */
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


    /* read from position filepos up to len bytes from buffer starting at offset off */
    public long Write(long filepos, byte[] buff, int off, int len) {
    	return writeFile(nHandle, filepos, buff, off, len);
    }

    public long Close() {
    	return closeFile(nHandle);
    }

    public long Sync() {
    	return syncFile(nHandle);
    }

    public static void main (String[] args) throws Exception {
		System.out.println("hello");
	
		XrdClFile tt = new XrdClFile();
	
		long status = tt.Open("root://eosuser//eos/user/t/tobbicke/TeSt", 0, 0);
		System.out.println("main open: status="+status);
	
		byte buff[] = new byte[8192];
		long len = tt.Read(0L, buff, 0, 8192);
	
		System.out.println("read " + len + " bytes");
		System.out.println(new String(buff));
	
		tt = null;
    }
}

