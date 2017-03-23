package ch.cern.awg.eos;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.TokenIdentifier;

public class Krb5TokenIdentifier extends TokenIdentifier {

	private UserGroupInformation ugi = null;
	public static final Text KIND_NAME = new Text("krb5");

	public Krb5TokenIdentifier() {
	}

	public UserGroupInformation getUser() {
		if (ugi == null) {
			try {
				ugi = UserGroupInformation.getCurrentUser();
			} catch (IOException e) {
				ugi = null;
			}
		}
		;
		return ugi;
	}

	public Text getKind() {
		return KIND_NAME;
	}

	public void readFields(DataInput in) throws IOException {

	}

	public void write(DataOutput out) throws IOException {

	}
};
