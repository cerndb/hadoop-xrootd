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

import org.apache.hadoop.fs.*;
import java.io.IOException;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.fs.FileSystem.Statistics;

class XrootDBasedInputStream extends FSInputStream implements Seekable, PositionedReadable {

    private XrootDBasedClFile file;
    private DebugLogger eosDebugLogger;

    private long pos = 0;
    private static final int IO_SIZE = 1024*1024;
    private final Statistics stats;

    public XrootDBasedInputStream(String url, Statistics stats) {
        this.eosDebugLogger = new DebugLogger(System.getenv("EOS_debug") != null);
        this.stats = stats;

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
        if (this.pos < 0) {  // TODO: implement - https://github.com/grahamar/hadoop-aws/blob/master/src/main/java/org/apache/hadoop/fs/s3a/S3AInputStream.java#L364
            this.eosDebugLogger.printDebug("EOSInputStream.read() pos: " + this.pos);

            return (int)this.pos;
        }

        long rd = file.Read(pos, b, off, len);
        this.eosDebugLogger.printDebug("EOSInputStream.read(pos=" + pos + ", b, off=" + off + ", len=" + len + ") readBytes: " + rd);
        if (rd >= 0) {
            this.pos = pos + rd;
            updateStatsBytesRead(rd);
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

        this.eosDebugLogger.printDebug("EOSInputStream.readFully(long pos, byte[] b)..");
        while (this.pos >= 0)
        {
            byte[] a = new byte[IO_SIZE];
            long rd = read(pos,a,0, IO_SIZE);
            pos = this.pos;
            System.arraycopy(a, 0, b, apos, (int)rd);
            apos += rd;
        }
        this.eosDebugLogger.printDebug("EOSInputStream.readFully(long pos, byte[] b) bytes read: " + apos);
    }

    public void readFully(long pos, byte[] buffer, int off, int len) throws IOException {
        this.eosDebugLogger.printDebug("EOSInputStream.readFully(long pos, byte[] buffer, int off, int len) pos: " + pos);
        read(pos,buffer,off,len);
    }

    public void seek(long pos) {
        this.eosDebugLogger.printDebug("EOSInputStream.seek(long pos) pos: " + pos);
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
        	eosDebugLogger.printDebug("close(): " + st);
        }
        pos = -2;
        super.close();
    }

    /**
     * Update (increment) the bytes read counter.
     * @param bytesRead number of bytes read
     */
    private void updateStatsBytesRead(long bytesRead) {
        if (stats != null && bytesRead > 0) {
            stats.incrementBytesRead(bytesRead);
        }
    }
}
