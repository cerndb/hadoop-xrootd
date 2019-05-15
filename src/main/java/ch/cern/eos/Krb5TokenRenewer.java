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
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenRenewer;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ccache.CCacheInputStream;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.ccache.FileCredentialsCache;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Krb5TokenRenewer extends TokenRenewer {
    public String krb5ccname;
    private static DebugLogger eosDebugLogger = new DebugLogger();

    public boolean handleKind(Text kind) {
        return Krb5TokenIdentifier.KIND_NAME.equals(kind);
    }

    public boolean isManaged(Token<?> token) throws IOException {
        return true;
    }

    public long renew(Token<?> token, Configuration conf) throws IOException {
        byte krb5cc[] = token.getPassword();
        int cc_version;

        eosDebugLogger.printDebug("renew: token l=" + krb5cc.length);

        sun.security.krb5.internal.ccache.Credentials cccreds = null;

        try {
            CCacheInputStream ccis = new CCacheInputStream(new ByteArrayInputStream(krb5cc));
            cc_version = ccis.readVersion();

            PrincipalName principal = ccis.readPrincipal(cc_version);
            eosDebugLogger.printDebug("renew: version " + cc_version + " principal " + principal.toString());

            Method readCred = ccis.getClass().getDeclaredMethod("readCred", int.class);
            readCred.setAccessible(true);

            if (eosDebugLogger.isDebugEnabled()) {
                Field ccisDebug = ccis.getClass().getDeclaredField("DEBUG");
                ccisDebug.setAccessible(true);
                ccisDebug.setBoolean(ccis, true);
            }

            while (ccis.available() > 0) {
                sun.security.krb5.internal.ccache.Credentials cr = null;
                eosDebugLogger.printDebug("renew: reading credentials version " + cc_version);
                try {
                    cr = (sun.security.krb5.internal.ccache.Credentials) readCred.invoke(ccis, cc_version);
                } catch (Exception e) {
                    e.printStackTrace();
                    eosDebugLogger.print("Failed to read Credentials version " + cc_version + ": " + e.getMessage());
                }
                eosDebugLogger.printDebug("renew: read credentials for " + cr.getServicePrincipal().toString());

                if (cr != null && cr.getServicePrincipal().getName().startsWith("krbtgt")) {
                    String[] nameStrings = cr.getServicePrincipal().getNameStrings();
                    eosDebugLogger.printDebug("renew: nameStrings " + Arrays.toString(nameStrings));

                    if (nameStrings[1].equals(cr.getServicePrincipal().getRealm().toString())) {
                        cccreds = cr;
                        break;
                    }
                }
            }
            ccis.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("no valid krb5 ticket");
        }

        try {
            sun.security.krb5.Credentials oldCreds = cccreds.setKrbCreds();
            sun.security.krb5.Credentials newCreds = oldCreds.renew();

            sun.security.krb5.internal.ccache.Credentials newCCcreds = new sun.security.krb5.internal.ccache.Credentials(
                    newCreds.getClient(), newCreds.getServer(), newCreds.getSessionKey(),
                    new KerberosTime(newCreds.getAuthTime()), new KerberosTime(newCreds.getStartTime()),
                    new KerberosTime(newCreds.getEndTime()), new KerberosTime(newCreds.getRenewTill()), false,
                    newCreds.getTicketFlags(), null, newCreds.getAuthzData(), newCreds.getTicket(), null);

            CredentialsCache fcc = null;
            krb5ccname = XRootDKrb5.krb5ccname;

            krb5ccname = ((krb5ccname.equals("")) ? "/tmp/krb_" + newCreds.getClient() : krb5ccname); //krb5ccname will be empty on RM

            eosDebugLogger.printDebug("TokenRenewer: krb5ccname " + krb5ccname);

            try {
                fcc = CredentialsCache.getInstance(newCreds.getClient(), krb5ccname);
            } catch (OutOfMemoryError e) {
                eosDebugLogger.print("failed to acquire existing CredentialsCache, allocating a new one");
                e.printStackTrace();
            }

            if (fcc == null) {
                eosDebugLogger.printDebug("renew: no credential cache for " + newCreds.getClient() + " in " + krb5ccname + ". Creating a new one...");

                fcc = CredentialsCache.create(newCreds.getClient(), krb5ccname);
                //krb5ccname = CredentialsCache.cacheName();
                if (fcc == null) eosDebugLogger.warn("renew: something went wrong wirh creating a new credentials");
            }
            ((FileCredentialsCache) fcc).version = cc_version;
            fcc.update(newCCcreds);
            fcc.save();

            /* write back to token as well */
            ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
            CCacheOutputStream ccos = new CCacheOutputStream(bos);
            ccos.writeHeader(newCreds.getClient(), cc_version);
            ccos.addCreds(newCCcreds);
            ccos.close();

            krb5cc = bos.toByteArray(); /* this is a copy (bos can be reused) */

            /*
             * update the token in-place using write and readFields: might as well have
             * updated the "password" field using reflection
             */
            bos.reset();
            DataOutputStream dos = new DataOutputStream(bos);
            WritableUtils.writeVInt(dos, token.getIdentifier().length);
            dos.write(token.getIdentifier());
            WritableUtils.writeVInt(dos, krb5cc.length);
            dos.write(krb5cc); /* the "password" */
            token.getKind().write(dos);
            token.getService().write(dos);
            dos.close();
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
            token.readFields(dis);
            eosDebugLogger.printDebug("Krb5TokenRenewer updated token " + token);

            long ticketLife = newCCcreds.getEndTime().getTime() - System.currentTimeMillis();
            eosDebugLogger.printDebug("Krb5TokenRenewer renewed " + fcc.cacheName() + " for "
                    + fcc.getPrimaryPrincipal().toString() + " ticketLife " + ticketLife
                    + " till time " + newCCcreds.getEndTime().getTime());

            return newCCcreds.getEndTime().getTime();

        } catch (KrbException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void cancel(Token<?> token, Configuration conf) throws IOException {
        byte krb5cc[] = token.getPassword();
        eosDebugLogger.printDebug("cancel: token l=" + krb5cc.length);
    }
};
