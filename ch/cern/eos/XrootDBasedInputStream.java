/* 
 * Author: CERN IT
 */
package ch.cern.eos;

import java.io.DataInputStream;
import java.io.FilterInputStream;
import org.apache.hadoop.fs.FSInputStream;

import java.io.IOException;
import java.io.EOFException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;

import ch.cern.eos.XrootDBasedClFile;

class XrootDBasedInputStream extends FSInputStream implements Seekable, PositionedReadable {

    private XrootDBasedClFile file;
    private DebugLogger eosDebugLogger;

    private long pos = 0;
    private static final int IO_SIZE = 1024*1024;

    public XrootDBasedInputStream(String url) {
        this.eosDebugLogger = new DebugLogger(System.getenv("EOS_debug") != null);
    
        this.file = new XrootDBasedClFile();
        long status = file.Open(url, 0, 0);
        
        if (status != 0) {
        	eosDebugLogger.printDebug("open " + url + " status=" + status);
        }
    }

    public long getPos() {
	    return this.pos;
    }

    public int read() throws IOException {
        // Reads the next byte of data from the input stream.
        byte[] b = new byte[1];

        long rd = read(this.pos, b, 0, 1);
        if (rd > 0) {
        	return b[0] & 0xFF;
        }
        if (rd == -1) {
        	return -1;
        }
        
        throw new IOException("read returned " + rd);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return read(this.pos, b, off, len);
    }

    public int read(long pos, byte[] b, int off, int len) throws IOException {
        //this.pos = pos;

        if (this.pos < 0) {  // TODO: This is unclear
            this.eosDebugLogger.printDebug("EOSInputStream.read() pos: " + this.pos);

            //if (this.pos == -1) { throw new EOFException(); }
            return (int)this.pos;
        }

        long rd = file.Read(pos, b, off, len);
        this.eosDebugLogger.printDebug("EOSInputStream.read() bytes: " + rd);
        if (rd >= 0) {
            this.pos = pos + rd;
            return (int) rd;
        }
        else if (rd == -1 ){
            this.pos = -1;
            return -1;
        }

        this.pos = -2;
        throw new IOException("read returned " + rd);
    }

    public void readFully(long pos, byte[] b) throws IOException {
        int apos=0;

        this.pos=pos;
        this.eosDebugLogger.printDebug("EOSInputStream.(long pos, byte[] b) pos: " + pos);
        while (this.pos >= 0)
        {
            byte[] a = new byte[IO_SIZE];
            long rd = read(pos,a,0, IO_SIZE);
            pos = this.pos;
            System.arraycopy(a, 0, b, apos, (int)rd);
            apos += rd;
        }
        this.eosDebugLogger.printDebug("EOSInputStream.(long pos, byte[] b) bytes read: " + apos);
    }

    public void readFully(long pos, byte[] buffer, int off, int len) throws IOException {
        this.eosDebugLogger.printDebug("EOSInputStream.readFully(long pos, byte[] buffer, int off, int len) pos: " + pos);
        read(pos,buffer,off,len);
    }

    public void seek(long pos) {
	    this.pos = pos;
    }

    public boolean seekToNewSource(long targetPos) {
	    throw new IllegalArgumentException("seekToNewSource");
    }

    public void close() throws IOException {
        if (pos < -1) {
        	return;
        }

        long st = file.Close();
        if (st != 0) {
        	eosDebugLogger.print("close(): " + st);
        }
        pos = -2;
    }
}
