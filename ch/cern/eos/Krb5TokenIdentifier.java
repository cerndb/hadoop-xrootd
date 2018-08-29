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

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.delegation.AbstractDelegationTokenIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Krb5TokenIdentifier extends AbstractDelegationTokenIdentifier {
    
    private UserGroupInformation ugi = null;
    public static final Text KIND_NAME = new Text("krb5");

    public Krb5TokenIdentifier() {
    }

    public UserGroupInformation xxgetUser() {
        if (ugi == null) {
            try {
                ugi = UserGroupInformation.getCurrentUser();
            } catch (IOException e) {
                ugi = null;
            }
        }
        return ugi;
    }

    public Text getKind() {
	    return KIND_NAME;
    }

    public void xxreadFields(DataInput in) throws IOException {
    }

    public void xxwrite(DataOutput out) throws IOException {
    }
};