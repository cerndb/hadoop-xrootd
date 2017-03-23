package ch.cern.awg.eos;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;

class EOSInputStream extends InputStream implements Seekable,
		PositionedReadable {

	private static final Log LOG = LogFactory.getLog(EOSInputStream.class);
	private XrdClFile file;
	private boolean EOS_debug = false;

	private long pos;

	public EOSInputStream(String url) throws IOException {
		if (System.getenv("EOS_debug") != null)
			EOS_debug = true;

		file = new XrdClFile();
		long status = file.Open(url, 0, 0);
		if (status != 0) {
			System.out.println("open " + url + " status=" + status);
			throw new IOException("Open status " + status + ": "
					+ EOSFileSystem.getErrText(status));
		}

		pos = 0;
	}

	public long getPos() {
		return pos;
	}

	public int read() {
		throw new IllegalArgumentException("read() not implemented");
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (pos < 0) {
			if (EOS_debug)
				System.out.println("EOSInputStream.read() pos: " + pos);

			if (pos == -1)
				throw new EOFException();
			return (int) pos;
			// throw new IOException("Stream closed");
		}

		long rd = file.Read(pos, b, off, len);
		if (EOS_debug)
			System.out.println("EOSInputStream.read() bytes: " + rd);
		if (rd >= 0) {
			pos += rd;
			if (rd > 0)
				return (int) rd;
			pos = -1;
			return -1;
		}

		pos = -2;
		throw new IOException("read returned " + rd);
	}

	public int read(long pos, byte[] b, int off, int len) throws IOException {
		throw new IllegalArgumentException(
				"read(pos, b, off, len) not implemented");
	}

	public void readFully(long pos, byte[] b) throws IOException {
		throw new IllegalArgumentException("readFully(pos, b) not implemented");
	}

	public void readFully(long pos, byte[] buffer, int off, int len)
			throws IOException {
		throw new IllegalArgumentException(
				"readFully(pos, b, off, len) not implemented");
	}

	public void seek(long pos) {
		this.pos = pos;
	}

	public boolean seekToNewSource(long targetPos) {
		throw new IllegalArgumentException("seekToNewSource");
	}

	public void close() throws IOException {
		if (pos < -1)
			return;

		long st = file.Close();
		if (st != 0)
			System.out.println("close(): " + st);
		pos = -2;
	}

}
