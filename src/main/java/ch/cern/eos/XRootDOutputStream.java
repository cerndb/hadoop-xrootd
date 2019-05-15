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

import org.apache.hadoop.fs.permission.FsPermission;

import java.io.IOException;
import java.io.OutputStream;

class XRootDOutputStream extends OutputStream {

    private DebugLogger eosDebugLogger = new DebugLogger();
    private XRootDClFile file;
    private long pos;

    public XRootDOutputStream(String url, FsPermission permission, boolean overwrite) {
        /* OpenFlags:
         *  None     = 0
         *  kXR_delete   = 2
         *  kXR_new      = 8
         *  kXR_open_read= 16
         *  kXR_open_updt= 32
         *  kXR_mkpath   = 256
         *  kXR_open_apnd= 512
         *  kXR_open_wrto= 32768
         */

        int oflags = overwrite ? 2 : 8;

        this.file = new XRootDClFile();
        long status = file.Open(url, oflags, 0x0180);
        this.eosDebugLogger.printDebug("EOSOutputStream create " + url + " status=" + status);

        this.pos = 0;
    }

    public long getPos() {
        return this.pos;
    }

    public void flush() throws IOException {
        long st = this.file.Sync();
        if (st != 0) {
            throw new IOException("flush() failed: " + st);
        }
    }

    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) b;

        write(0L, buf, 0, 1);
        this.pos++;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (this.pos < 0) {
            throw new IOException("Stream closed");
        }

        long st = this.file.Write(pos, b, off, len);
        if (st != 0) {
            throw new IOException("write " + len + " bytes error " + st);
        }

        this.eosDebugLogger.printDebug("EOSInputStream.write(pos=" + pos + ", b, off=" + off + ", len=" + len + ")");
        this.pos += len;
    }

    public void close() throws IOException {
        if (this.pos < 0) {
            return;
        }

        long st = this.file.Close();
        if (st != 0) {
            throw new IOException("close() failed: " + st);
        }

        if (st == 0) {
            this.pos = -1;
        }
    }

    public void write(long pos, byte[] b, int off, int len) throws IOException {
        long st = this.file.Write(pos, b, off, len);
        if (st != 0) {
            throw new IOException("write failed error " + st);
        }

        this.eosDebugLogger.printDebug("EOSInputStream.write(pos=" + pos + ", b, off=" + off + ", len=" + len + ")");
    }

    public void seek(long pos) {
        this.pos = pos;
    }
}
