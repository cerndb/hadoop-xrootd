package ch.cern.eos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.Class;
import java.lang.Integer;
import java.lang.OutOfMemoryError;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.System;

import java.nio.file.Files;

import java.util.Arrays;
import java.util.Vector;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;

/*import sun.security.krb5.internal.ccache.Krb5ByteCCache;*/

import sun.security.krb5.internal.ccache.CCacheInputStream;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.ccache.FileCredentialsCache;
import sun.security.krb5.internal.ccache.Credentials;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.ccache.FileCCacheConstants;
import sun.security.krb5.internal.ccache.Tag;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenRenewer;


public class Krb5TokenRenewer extends TokenRenewer {


    public boolean handleKind(Text kind) {
	return Krb5TokenIdentifier.KIND_NAME.equals(kind);
    }

    public boolean isManaged(Token<?> token) throws IOException {
	return true;
    }

    public String krb5ccname;		    /* needed by EOSFileSystem */
    public long renew(Token<?> token, Configuration conf) throws IOException {

	String prop_EOS_debug = System.getProperty("EOS_debug");
	boolean EOS_debug = (prop_EOS_debug != null) && (prop_EOS_debug.equals("true"));
	byte krb5cc[] = token.getPassword();

	int cc_version;

	if (EOS_debug) System.out.println("renew: token l=" + krb5cc.length);

	sun.security.krb5.internal.ccache.Credentials cccreds = null;

	try {
	    CCacheInputStream ccis = new CCacheInputStream(new ByteArrayInputStream(krb5cc));
	    cc_version = ccis.readVersion();

	    PrincipalName principal = ccis.readPrincipal(cc_version);
	    if (EOS_debug) System.out.println("renew: version " + cc_version + " principal " + principal.toString());

	    Method readCred = ccis.getClass().getDeclaredMethod("readCred", int.class);
	    readCred.setAccessible(true);

	    if (EOS_debug) {
		Field ccisDebug = ccis.getClass().getDeclaredField("DEBUG"); 
		ccisDebug.setAccessible(true);
		ccisDebug.setBoolean(ccis, true);
	    }

	    while (ccis.available() > 0) {
		sun.security.krb5.internal.ccache.Credentials cr = null;
		if (EOS_debug) System.out.println("renew: reading credentials version " + cc_version);
		try {
		    cr = (sun.security.krb5.internal.ccache.Credentials) readCred.invoke(ccis, cc_version);
		} catch (Exception e) {
		    e.printStackTrace();
		    System.out.println("Failed to read Credentials version " + cc_version + ": " + e.getMessage());
		}
		if (EOS_debug) System.out.println("renew: read credentials for " + cr.getServicePrincipal().toString());

		if (cr != null && cr.getServicePrincipal().getName().startsWith("krbtgt")) {
		    String[] nameStrings = cr.getServicePrincipal().getNameStrings();
		    if (EOS_debug) System.out.println("renew: nameStrings " + Arrays.toString(nameStrings));

		    if (nameStrings[1].equals(cr.getServicePrincipal().getRealm().toString())) {
			cccreds = cr;
			break;
		    }
		}
	    }
	    ccis.close();

	} catch(Exception e) {
	    e.printStackTrace();
	    throw new IOException("no valid krb5 ticket");
	}

	/* Krb5ByteCCache krb5CC = new Krb5ByteCCache(new ByteArrayInputStream(krb5cc));*/

	try {

	    sun.security.krb5.Credentials oldCreds = cccreds.setKrbCreds();
	    sun.security.krb5.Credentials newCreds = oldCreds.renew();

	    sun.security.krb5.internal.ccache.Credentials newCCcreds = new sun.security.krb5.internal.ccache.Credentials(
		    newCreds.getClient(), newCreds.getServer(), newCreds.getSessionKey(),
		    new KerberosTime(newCreds.getAuthTime()), new KerberosTime(newCreds.getStartTime()),
		    new KerberosTime(newCreds.getEndTime()), new KerberosTime(newCreds.getRenewTill()),
		    false, newCreds.getTicketFlags(), null, newCreds.getAuthzData(), newCreds.getTicket(),
		    null);

	    CredentialsCache fcc = null;
	    EOSFileSystem.initLib();		    /* most often a no-op */
//	    krb5ccname = EOSFileSystem.getenv("KRB5CCNAME");
            krb5ccname = EOSKrb5.krb5ccname;
            System.out.println("TokenRenewer: krb5ccname " +krb5ccname);

	    try { 
		fcc = CredentialsCache.getInstance(newCreds.getClient(), krb5ccname);
	    } catch(OutOfMemoryError e) {
		System.out.println("failed to acquire existing CredentialsCache, allocating a new one");
		e.printStackTrace();
	    }

	    if (fcc == null) fcc = CredentialsCache.create(newCreds.getClient(), krb5ccname);
	    ((FileCredentialsCache) fcc).version = cc_version;
	    fcc.update(newCCcreds);
	    fcc.save();

	    /* write back to token as well */
	    ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
	    CCacheOutputStream ccos = new CCacheOutputStream(bos);
	    ccos.writeHeader(newCreds.getClient(), cc_version);
	    ccos.addCreds(newCCcreds);
	    ccos.close();

	    krb5cc = bos.toByteArray();		/* this is a copy (bos can be reused) */

	    /* update the token in-place using write and readFields: might as well have updated the "password" field using reflection */
	    bos.reset();
	    DataOutputStream dos = new DataOutputStream(bos);
	    WritableUtils.writeVInt(dos, token.getIdentifier().length);
	    dos.write(token.getIdentifier());
	    WritableUtils.writeVInt(dos, krb5cc.length);
	    dos.write(krb5cc);			/* the "password" */
	    token.getKind().write(dos);
	    token.getService().write(dos);
	    dos.close();
	    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
	    token.readFields(dis);
	    if (EOS_debug) System.out.println("Krb5TokenRenewer updated token " + token);

	    long ticketLife = newCCcreds.getEndTime().getTime() - System.currentTimeMillis();
	    if (EOS_debug) System.out.println("Krb5TokenRenewer renewed " + fcc.cacheName() + " for " + fcc.getPrimaryPrincipal().toString() + " ticketLife " + ticketLife);

	    return ticketLife;

	} catch(KrbException e) {
	    throw new IOException(e.getMessage());
	}

    }

    public void cancel(Token<?> token, Configuration conf) throws IOException {
    }
};

