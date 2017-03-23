package ch.cern.awg.eos;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenRenewer;

import sun.security.krb5.KrbException;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ccache.FileCredentialsCache;
import sun.security.krb5.internal.ccache.Krb5ByteCCache;

public class Krb5TokenRenewer extends TokenRenewer {

	public boolean handleKind(Text kind) {
		return Krb5TokenIdentifier.KIND_NAME.equals(kind);
	}

	public boolean isManaged(Token<?> token) throws IOException {
		return true;
	}

	public String krb5ccname;

	public long renew(Token<?> token, Configuration conf) throws IOException {
		byte krb5cc[] = token.getPassword();

		Krb5ByteCCache krb5CC = new Krb5ByteCCache(new ByteArrayInputStream(
				krb5cc));

		try {
			sun.security.krb5.Credentials newCreds = krb5CC.getDefaultCreds()
					.setKrbCreds().renew();

			sun.security.krb5.internal.ccache.Credentials newCCcreds = new sun.security.krb5.internal.ccache.Credentials(
					newCreds.getClient(), newCreds.getServer(),
					newCreds.getSessionKey(), new KerberosTime(
							newCreds.getAuthTime()), new KerberosTime(
							newCreds.getStartTime()), new KerberosTime(
							newCreds.getEndTime()), new KerberosTime(
							newCreds.getRenewTill()), false,
					newCreds.getTicketFlags(), null, newCreds.getAuthzData(),
					newCreds.getTicket(), null);

			FileCredentialsCache fcc = FileCredentialsCache.acquireInstance();
			fcc.update(newCCcreds);
			fcc.save();
			krb5ccname = fcc.cacheName();

			long ticketLife = System.currentTimeMillis()
					- newCreds.getEndTime().getTime();
			System.out.println("Krb5TokenRenewer renewed " + fcc.cacheName()
					+ " for " + fcc.getPrimaryPrincipal().toString()
					+ " ticketLife " + ticketLife);
			return ticketLife;

		} catch (KrbException e) {
			throw new IOException(e.getMessage());
		}

	}

	public void cancel(Token<?> token, Configuration conf) throws IOException {
	}
};
