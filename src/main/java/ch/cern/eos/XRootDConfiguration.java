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

    public int getReadBufferSize() throws IllegalArgumentException {
        int readBufferSize;
        // if the designated environment variable is set use this as read ahead size
        java.lang.String envReadBufferValue = System.getenv(XRootDConstants.OS_ENV_VARIABLE_READ_BUFFER);
        if (envReadBufferValue != null ) {
            readBufferSize = Integer.parseInt(envReadBufferValue);
            eosDebugLogger.printDebug("The OS environment variable " +
                    XRootDConstants.OS_ENV_VARIABLE_READ_BUFFER  + " = " + readBufferSize);
            if (readBufferSize < 0) {
                throw new IllegalArgumentException(String.format("Config %s=%d is below the minimum value %d",
                        XRootDConstants.OS_ENV_VARIABLE_READ_BUFFER, readBufferSize, 0));
            }
            // otherwise get read ahead value from Hadoop configuration or use default if not set
        } else {
            readBufferSize = XRootDConfiguration.byteConfOption(this.conf, XRootDConstants.READ_BUFFER_SIZE,
                    XRootDConstants.DEFAULT_READ_BUFFER_SIZE);
            if (this.conf.get(XRootDConstants.READ_BUFFER_SIZE) == null) {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.READ_BUFFER_SIZE +
                        " nor OS environment variable " + XRootDConstants.OS_ENV_VARIABLE_READ_BUFFER +
                        " are set, using default value = " +
                        XRootDConstants.DEFAULT_READ_BUFFER_SIZE);
            }
            else {
                eosDebugLogger.printDebug("Hadoop Config " + XRootDConstants.READ_BUFFER_SIZE + " = " + readBufferSize);
            }
        }

        return readBufferSize;
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
