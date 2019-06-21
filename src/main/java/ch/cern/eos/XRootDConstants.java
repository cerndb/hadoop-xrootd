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

public final class XRootDConstants {

    /**
     * read ahead buffer size to prevent connection re-establishments.
     */
    public static final String READ_BUFFER_SIZE = "fs.xrootd.read.buffer";
    public static final int DEFAULT_READ_BUFFER_SIZE = 128 * 1024;
    public static final String OS_ENV_VARIABLE_READ_BUFFER = "XROOTDHADOOP_READ_BUFFER_SIZE";

    /**
     * write ahead buffer size.
     */
    public static final String WRITE_BUFFER_SIZE = "fs.xrootd.write.buffer";
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 1024;
    public static final String OS_ENV_VARIABLE_WRITE_BUFFER = "XROOTDHADOOP_WRITE_BUFFER_SIZE";
}
