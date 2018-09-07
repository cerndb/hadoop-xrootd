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

import java.io.IOException;
import java.net.URI;

public class XRootDKrb5FileSystem extends XRootDFileSystem {

	public XRootDKrb5FileSystem() {
		super();
	}
	
	protected void initHandle() throws IOException {
		super.initHandle();
		setkrbcc(XRootDKrb5.setKrb());
    }
	
	public void initialize(URI uri, Configuration conf) throws IOException {
		super.initialize(uri, conf);
		setkrbcc(XRootDKrb5.setKrb());
    }

    /*
     * Setting token cache from TGT (on Spark or MR drivers) or init local krb cache from token 
     * (if mapper or executor) 
     */
    public static void setKrb() {
		XRootDKrb5.setKrb();
    }

    /* 
     * This sets (setenv()) KRB5CCNAME in the current (!) environment, 
     * which is NOT the one java currently sees, nor the one a java sub-process is going to see spawned
     * using execve() - for the latter one would have to modify java's copy of the environment which is doable.
     * jython or scala may yet play different games
     */
    public static void setkrbcc(String ccname) throws IOException {
		XRootDFileSystem.initLib();
		setenv("KRB5CCNAME", "FILE:" + ccname);
    }
}
