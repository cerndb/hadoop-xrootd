package ch.cern.eos;

import java.io.DataInputStream;
import java.io.FilterInputStream;
//import java.io.InputStream;
import org.apache.hadoop.fs.FSInputStream;
//
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
//    private boolean EOS_debug = false;
    private boolean EOS_debug = true;

    private long pos;

    public EOSInputStream(String url) {
	if (System.getenv("EOS_debug") != null) EOS_debug = true;

	file = new XrdClFile();
	long status = file.Open(url, 0, 0);
	if (status != 0) System.out.println("open " + url + " status=" + status);

	pos = 0;
    }

    public long getPos() {
	return pos;
    }

    public int read() throws IOException{


        if (pos < 0) {
            if (EOS_debug) System.out.println("EOSInputStream.read() pos: " + pos);

            if (pos == -1) throw new EOFException();
            return (int) pos;

        }
//Reads the next byte of data from the input stream.
       byte[] b= new byte[1];
       long rd = file.Read(pos, b, 0, 1);
       if (rd >= 0) {
            pos += rd;
            if (rd > 0) return b[0] & 0xFF ;
            pos = -1;
            return -1;
        }

        pos = -2;
        throw new IOException("read returned " + rd);
         
       
       
//	throw new IllegalArgumentException("read() not implemented");
		
    }

    public int read(byte[] b, int off, int len) throws IOException {
	if (pos < 0) {
	    if (EOS_debug) System.out.println("EOSInputStream.read() pos: " + pos);

	    if (pos == -1) throw new EOFException();
	    return (int) pos;
            
	    // throw new IOException("Stream closed");
	}

       	long rd = file.Read(pos, b, off, len);
	if (EOS_debug) System.out.println("EOSInputStream.read() bytes: " + rd);
	if (rd >= 0) {
	    pos += rd;
	    if (rd > 0) return (int) rd;
	    pos = -1;
	    return -1;
	}

	pos = -2;
	throw new IOException("read returned " + rd);
    }

    public int read(long pos, byte[] b, int off, int len) throws IOException {
        if (pos < 0) {
            if (EOS_debug) System.out.println("EOSInputStream.read() pos: " + pos);

            if (pos == -1) throw new EOFException();
            return (int) pos;
        }

        long rd = file.Read(pos, b, off, len);
        if (EOS_debug) System.out.println("EOSInputStream.read() bytes: " + rd);
        if (rd >= 0) {
            this.pos = pos += rd;
            if (rd > 0) return (int) rd;
            this.pos = -1;
            return -1;
        }

        this.pos = -2;
        throw new IOException("read returned " + rd);

        

//	throw new IllegalArgumentException("read(pos, b, off, len) not implemented");
    }

    public void readFully(long pos, byte[] b) throws IOException {
        int apos=0;
        int io_size=1024*1024;
        this.pos=pos;
        if (EOS_debug) System.out.println("EOSInputStream.(long pos, byte[] b) pos: " + pos);
        while(this.pos>=0)
	{
	    byte[] a = new byte[io_size];
	    long rd = read(this.pos,a,0,io_size);
            System.arraycopy(a, 0, b, apos, (int)rd);
            apos+=rd;
        }
        if (EOS_debug) System.out.println("EOSInputStream.(long pos, byte[] b) bytes read: " + apos);

//	throw new IllegalArgumentException("readFully(pos, b) not implemented");
    }

    public void readFully(long pos, byte[] buffer, int off, int len) throws IOException {
        if (EOS_debug) System.out.println("EOSInputStream.readFully(long pos, byte[] buffer, int off, int len) pos: " + pos);

        read(pos,buffer,off,len);
//	throw new IllegalArgumentException("readFully(pos, b, off, len) not implemented");
    }

    public void seek(long pos) {
	this.pos = pos;
    }

    public boolean seekToNewSource(long targetPos) {
	throw new IllegalArgumentException("seekToNewSource");
    }

    public void close() throws IOException {
	if (pos < -1) return;

	long st = file.Close();
	if (st != 0) System.out.println("close(): " + st);
	pos = -2;
    }

}
