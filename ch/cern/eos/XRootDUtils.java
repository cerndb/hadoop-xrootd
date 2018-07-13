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

public final class XRootDUtils {

    /**
     * Get a long option >= the minimum allowed value, supporting memory such as prefixes K,M,G,T,P.
     * @param conf configuration
     * @param confKey key to look up
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
