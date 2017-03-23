package sun.security.krb5.internal.ccache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import sun.security.krb5.Asn1Exception;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.Realm;
import sun.security.krb5.RealmException;
import sun.security.krb5.internal.LoginOptions;

public class Krb5ByteCCache extends CredentialsCache {

	private Vector<sun.security.krb5.internal.ccache.Credentials> credentialsList;
	private PrincipalName principal;
	private int version;

	public ByteArrayOutputStream bos;

	public Krb5ByteCCache() {
	};

	public Krb5ByteCCache(InputStream bs) throws IOException {

		this();

		try {
			CCacheInputStream ccis = new CCacheInputStream(bs);
			this.version = ccis.readVersion();
			this.principal = ccis.readPrincipal(this.version);

			while (ccis.available() > 0) {
				sun.security.krb5.internal.ccache.Credentials cred = ccis
						.readCred(this.version);

				if (cred != null) {
					this.credentialsList.addElement(cred);
				}
			}
			ccis.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("no valid krb5 ticket");
		}

	}

	public sun.security.krb5.internal.ccache.Credentials getCreds(
			LoginOptions options, PrincipalName sname, Realm srealm) {
		if (options == null)
			return getCreds(sname, srealm);

		sun.security.krb5.internal.ccache.Credentials[] list = getCredsList();
		try {
			for (int i = 0; i < list.length; i++) {
				if (sname.match(list[i].getServicePrincipal())
						&& (srealm.toString().equals(list[i]
								.getServicePrincipal().getRealm().toString()))) {
					if (list[i].getTicketFlags().match(options))
						return list[i];
				}
			}
		} catch (RealmException e) {
		}
		;

		return null;
	}

	public sun.security.krb5.internal.ccache.Credentials getCreds(
			PrincipalName sname, Realm srealm) {
		sun.security.krb5.internal.ccache.Credentials[] list = getCredsList();

		if (list != null) {
			try {
				for (int i = 0; i < list.length; i++) {
					if (sname.match(list[i].getServicePrincipal())
							&& (srealm.toString().equals(list[i]
									.getServicePrincipal().getRealm()
									.toString()))) {
						return list[i];
					}
				}
			} catch (RealmException e) {
			}
			;
		}

		return null;
	}

	public sun.security.krb5.internal.ccache.Credentials getDefaultCreds() {
		sun.security.krb5.internal.ccache.Credentials[] list = getCredsList();

		if (list != null) {
			try {
				for (int i = list.length - 1; i >= 0; i--) {
					if (list[i].getServicePrincipal().getName()
							.startsWith("krbtgt")) {
						String[] nameStrings = list[i].getServicePrincipal()
								.getNameStrings();
						// find the TGT for the current realm krbtgt/realm@realm
						if (nameStrings[1].equals(list[i].getServicePrincipal()
								.getRealm().toString())) {
							return list[i];
						}
					}
				}
			} catch (RealmException e) {
			}
		}
		return null;
	}

	public sun.security.krb5.internal.ccache.Credentials[] getCredsList() {
		if ((credentialsList == null) || (credentialsList.isEmpty()))
			return null;

		sun.security.krb5.internal.ccache.Credentials[] tmp = new sun.security.krb5.internal.ccache.Credentials[credentialsList
				.size()];

		for (int i = 0; i < credentialsList.size(); i++) {
			tmp[i] = credentialsList.elementAt(i);
		}
		return tmp;
	}

	public void save() throws IOException, Asn1Exception {
		bos = new ByteArrayOutputStream(4096);

		CCacheOutputStream ccos = new CCacheOutputStream(bos);
		ccos.writeHeader(principal, version);
		ccos.addCreds(getDefaultCreds());
		ccos.close();

	}

	public byte[] getBytes() {
		return bos.toByteArray();
	}

	static boolean DEBUG = true;

	public void update(sun.security.krb5.internal.ccache.Credentials c) {

		if (credentialsList != null) {
			if (credentialsList.isEmpty()) {
				credentialsList.addElement(c);
			} else {
				Credentials tmp = null;
				boolean matched = false;

				for (int i = 0; i < credentialsList.size(); i++) {
					tmp = credentialsList.elementAt(i);
					if (match(c.sname.getNameStrings(),
							tmp.sname.getNameStrings())
							&& ((c.sname.getRealmString())
									.equalsIgnoreCase(tmp.sname
											.getRealmString()))) {
						matched = true;
						if (c.endtime.getTime() >= tmp.endtime.getTime()) {
							if (DEBUG) {
								System.out
										.println(" >>> FileCredentialsCache Ticket matched, overwrite the old one.");
							}
							credentialsList.removeElementAt(i);
							credentialsList.addElement(c);
						}
					}
				}
				if (matched == false) {
					if (DEBUG) {
						System.out
								.println(" >>> FileCredentialsCache Ticket  not exactly matched, add new one into cache.");
					}

					credentialsList.addElement(c);
				}
			}
		}
	}

	boolean match(String[] s1, String[] s2) {
		if (s1.length != s2.length) {
			return false;
		} else {
			for (int i = 0; i < s1.length; i++) {
				if (!(s1[i].equalsIgnoreCase(s2[i]))) {
					return false;
				}
			}
		}
		return true;
	}

	public PrincipalName getPrimaryPrincipal() {
		return principal;
	}

	@Override
	public Credentials getCreds(PrincipalName arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credentials getCreds(LoginOptions arg0, PrincipalName arg1) {
		// TODO Auto-generated method stub
		return null;
	}
}
