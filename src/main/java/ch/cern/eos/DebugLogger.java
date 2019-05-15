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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DebugLogger {

    private Logger logger = LogManager.getLogger("ch.cern.eos.HadoopXRootD");

    public void print(String e) {
        this.logger.info(e);
    }

    public void warn(String e) {
        this.logger.warn(e);
    }

    public void printDebug(String e) {
        this.logger.debug(e);
    }

    public void printWarn(String e) {
        this.logger.warn(e);
    }

    public void printStackTrace(Exception e) {
        this.logger.debug(e.getMessage());
        if (this.isDebugEnabled()) {
            e.printStackTrace();
        }
    }

    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }
}
