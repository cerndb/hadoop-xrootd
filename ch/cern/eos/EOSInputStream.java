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

import ch.cern.eos.XrdClFile;

class EOSInputStream extends FSInputStream implements Seekable, PositionedReadable {

    private static final Log LOG = LogFactory.getLog(EOSInputStream.class);
    private XrdClFile file;
    private EOSDebugLogger eosDebugLogger;

    private long pos = 0;
    private static final int IO_SIZE = 1024*1024;

    public EOSInputStream(String url) {
        this.eosDebugLogger = new EOSDebugLogger(System.getenv("EOS_debug") != null);
    
        this.file = new XrdClFile();
        long status = file.Open(url, 0, 0);
        
        if (status != 0) System.out.println("open " + url + " status=" + status);
    }

    public long getPos() {
	    return this.pos;
    }

    public int read() throws IOException {
        // Reads the next byte of data from the input stream.
        byte[] b = new byte[1];

        long rd = read(pos, b, 0, 1);
        if (rd > 0) return b[0] & 0xFF;
        if (rd == -1) return -1;
        
        throw new IOException("read returned " + rd);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return read(this.pos, b, off, len);
    }

    public int read(long pos, byte[] b, int off, int len) throws IOException {
        this.pos = pos;

        if (this.pos < 0) {
            this.eosDebugLogger.print("EOSInputStream.read() pos: " + this.pos);

            if (this.pos == -1) throw new EOFException();
            return (int)this.pos;
        }

        long rd = file.Read(this.pos, b, off, len);
        this.eosDebugLogger.print("EOSInputStream.read() bytes: " + rd);
        if (rd >= 0) {
            this.pos += rd;
            if (rd > 0) return (int) rd;
            this.pos = -1;
            return -1;
        }

        this.pos = -2;
        throw new IOException("read returned " + rd);
    }

    public void readFully(long pos, byte[] b) throws IOException {
        int apos=0;

        this.pos=pos;
        this.eosDebugLogger.print("EOSInputStream.(long pos, byte[] b) pos: " + pos);
        while (this.pos >= 0)
        {
            byte[] a = new byte[IO_SIZE];
            long rd = read(this.pos, a, 0, IO_SIZE);
            System.arraycopy(a, 0, b, apos, (int)rd);
            apos += rd;
        }
        this.eosDebugLogger.print("EOSInputStream.(long pos, byte[] b) bytes read: " + apos);
    }

    public void readFully(long pos, byte[] buffer, int off, int len) throws IOException {
        this.eosDebugLogger.print("EOSInputStream.readFully(long pos, byte[] buffer, int off, int len) pos: " + pos);
        read(pos,buffer,off,len);
    }

    public void seek(long pos) {
	    this.pos = pos;
    }

    public boolean seekToNewSource(long targetPos) {
	    throw new UnsupportedOperationException("seekToNewSource");
    }

    public void close() throws IOException {
        if (pos < -1) return;

        long st = file.Close();
        if (st != 0) System.out.println("close(): " + st);
        pos = -2;
    }
}
