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

public class XRootDClFile {
    private long nHandle;

    public XRootDClFile() {
        nHandle = initFile();
    }

    private native long initFile();

    private native long disposeFile(long handle);

    private native long openFile(long handle, String url, int flags, int mode);

    private native long readFile(long handle, long pos, byte buffer[], int off, int len);

    private native long writeFile(long handle, long pos, byte buffer[], int off, int len);

    private native long syncFile(long handle);

    private native long closeFile(long handle);

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

    /**
     * read from position in the file filepos up to len bytes
     * into buffer starting at offset off (buffer[off] -> buffer[off+len])
     */
    public long Read(long filepos, byte[] buff, int off, int len) {
        return readFile(nHandle, filepos, buff, off, len);
    }

    /**
     * write from position in the file filepos up to len bytes
     * from buffer starting at offset off (buffer[off] -> buffer[off+len])
     */
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

