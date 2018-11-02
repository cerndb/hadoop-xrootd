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

public class XRootDInstrumentation {

    private static Long timeElapsedReadMusec = 0L;
    private static int readOps = 0;
    private static Long bytesRead = 0L;

    /**
     * Get the cumulative value of the elapsed time spent  by
     * the Hadoop Filesystem client waiting for EOS/XRootD to return data of read
     * operations. The time is in microseconds.
     * Use from Hadoop clients, such as Spark environments, by calling
     * ch.cern.eos.XRootDInstrumentation.getTimeElapsedReadMusec()
     *
     * @return cumulative elapsed time spend in read operations, in microseconds.
     */
    public static long getTimeElapsedReadMusec() {
        return timeElapsedReadMusec;
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for EOS/XRootD to return data of read
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadMusec += incrementTime;
    }

    /**
     * Get the cumulative value of the number of bytes read  by
     * the Hadoop Filesystem client through the XRootD connector
     *
     * @return cumulative bytes read with read operations.
     */
    public static long getBytesRead() {
        return bytesRead;
    }

    /**
     * Increment the value of the cumulative number of bytes read by
     * the Hadoop Filesystem clients reading from EOS/XRootD
     *
     * @param incrementBytesRead number of bytes to add to the counter of bytes read.
     */
    public static void incrementBytesRead(Long incrementBytesRead) {
        bytesRead += incrementBytesRead;
    }

    /**
     * Get the cumulative value of the number of read operations performed by
     * the Hadoop Filesystem client through the XRootD connector.
     *
     * @return cumulative number of read operations.
     */
    public static int getReadOps() {
        return readOps;
    }

    /**
     * Increment the counter of the cumulative number of read operations performed by
     * the Hadoop Filesystem clients reading from EOS/XRootD.
     *
     * @param numOps increment the counter of read operations.
     */
    public static void incrementReadOps(int numOps) {
        readOps +=  numOps;
    }
}
