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

    private static Long timeElapsedReadOps = 0L;

    /**
     * Get the cumulative value of the elapsed time spent  by
     * Hadoop Filesystem clients waiting for EOS to return data of read
     * operations. The time is in microseconds.
     * Use from Hadoop clients, such as Spark environments, by calling
     * ch.cern.eos.XRootDInstrumentation.getTimeElapsedReadOps()
     *
     * @return cumulative elapsed read time in microseconds.
     */
    public static long getTimeElapsedReadOps() {
        return timeElapsedReadOps;
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for EOS to return data of read
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadOps += incrementTime;
    }

}

