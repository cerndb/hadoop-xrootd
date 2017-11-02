package ch.cern.eos;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.System;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.delegation.AbstractDelegationTokenIdentifier;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;

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