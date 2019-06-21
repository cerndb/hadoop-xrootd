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

import org.apache.hadoop.conf.Configuration;

public class XRootDConfiguration {

    private static DebugLogger eosDebugLogger = new DebugLogger();

    private Configuration conf;

    XRootDConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public int getReadAhead() throws IllegalArgumentException {
        int readAhead;
        // if the designated environment variable is set use this as read ahead size
        java.lang.String envReadaheadValue = System.getenv(XRootDConstants.OS_ENV_VARIABLE_READAHEAD);
        if (envReadaheadValue != null ) {
            readAhead = Integer.parseInt(envReadaheadValue);
            eosDebugLogger.printDebug("The OS environment variable " +
                    XRootDConstants.OS_ENV_VARIABLE_READAHEAD  + " = " + readAhead);
            if (readAhead < 0) {
                throw new IllegalArgumentException(String.format("Config %s=%d is below the minimum value %d",
                        XRootDConstants.OS_ENV_VARIABLE_READAHEAD, readAhead, 0));
            }
            // otherwise get read ahead value from Hadoop configuration or use default if not set
        } else {
            readAhead = XRootDConfiguration.byteConfOption(this.conf, XRootDConstants.READAHEAD_RANGE,
                    XRootDConstants.DEFAULT_READAHEAD_RANGE);
            if (this.conf.get(XRootDConstants.READAHEAD_RANGE) == null) {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.READAHEAD_RANGE +
                        " nor OS environment variable " + XRootDConstants.OS_ENV_VARIABLE_READAHEAD +
                        " are set, using default value = " +
                        XRootDConstants.DEFAULT_READAHEAD_RANGE);
            }
            else {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.READAHEAD_RANGE + " = " + readAhead);
            }
        }

        return readAhead;
    }

    public int getWriteBufferSize() throws IllegalArgumentException {
        int writeBufferSize;
        // if the designated environment variable is set use this as read ahead size
        java.lang.String envWriteBuffValue = System.getenv(XRootDConstants.OS_ENV_VARIABLE_WRITE_BUFFER);
        if (envWriteBuffValue != null ) {
            writeBufferSize = Integer.parseInt(envWriteBuffValue);
            eosDebugLogger.printDebug("The OS environment variable " +
                    XRootDConstants.OS_ENV_VARIABLE_WRITE_BUFFER + " = " + writeBufferSize);
            if (writeBufferSize < 0) {
                throw new IllegalArgumentException(String.format("Config %s=%d is below the minimum value %d",
                        XRootDConstants.OS_ENV_VARIABLE_WRITE_BUFFER, writeBufferSize, 0));
            }
            // otherwise get read ahead value from Hadoop configuration or use default if not set
        } else {
            writeBufferSize = XRootDConfiguration.byteConfOption(this.conf, XRootDConstants.WRITE_BUFFER_SIZE,
                    XRootDConstants.DEFAULT_WRITE_BUFFER_SIZE);
            if (this.conf.get(XRootDConstants.WRITE_BUFFER_SIZE) == null) {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.WRITE_BUFFER_SIZE +
                        " nor OS environment variable " + XRootDConstants.OS_ENV_VARIABLE_WRITE_BUFFER +
                        " are set, using default value = " + XRootDConstants.DEFAULT_WRITE_BUFFER_SIZE);
            }
            else {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.WRITE_BUFFER_SIZE + " = " + writeBufferSize);
            }
        }

        return writeBufferSize;
    }

    /**
     * Get a long option >= the minimum allowed value, supporting memory such as prefixes K,M,G,T,P.
     *
     * @param conf         configuration
     * @param confKey      key to look up
     * @param defaultValue default value
     * @return the value
     * @throws IllegalArgumentException if the value is below the minimum
     */
    static int byteConfOption(Configuration conf,
                              String confKey,
                              int defaultValue) {
        int confValue = conf.getInt(confKey, defaultValue);

        if (confValue < 0) {
            throw new IllegalArgumentException(String.format("Config %s=%d is below the minimum value %d",
                    confKey, confValue, 0));
        }
        return confValue;
    }
}
