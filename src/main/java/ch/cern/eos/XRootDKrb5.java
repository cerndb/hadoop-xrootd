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
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.Ticket;
import sun.security.krb5.internal.TicketFlags;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.ccache.Credentials;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.ccache.FileCCacheConstants;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;

public class XRootDKrb5 {
    public static String krb5ccname = "";
    public static int hasUgiKrbToken = -1;
    public static int hasKrbTGT = -1;
    public static int hasKrbST = -1;

    private static UserGroupInformation ugi;

    private static final String TOKEN_KRB_KIND = "krb5";
    private static final String TOKEN_YARN_KIND = "YARN_AM_RM_TOKEN";
    private static final String TOKEN_PRINCIPAL_XROOTD_PREFIX = "xrootd";

    private static int yarnExecutor = 0;
    private static DebugLogger eosDebugLogger = new DebugLogger();

    public synchronized static String setKrb() {
        // If no Krb ticket, set from Token. 
        // If no Krb Token, set from ticket.
        // FIXME: The block below requires refactoring and redesign
        int hadKrbTGT = hasKrbTGT, hadUgiKrbToken = hasUgiKrbToken, hadKrbST = hasKrbST;
        if (hasUgiKrbToken < 0 && hasKrbTGT < 0 && hasKrbST < 0) {
            // check for token if still initial state
            eosDebugLogger.printDebug("Check for UGI and ST tokens:");
            checkToken();
        }


        eosDebugLogger.printDebug("Set UGI tokens:");
        if (hasUgiKrbToken > 0 && yarnExecutor == 1) {            // we're most likely a M/R task or Spark executor
            if (hasKrbTGT > -10) {
                try {
                    krb5ccname = getUgiKrbTokenCache();
                } catch (IOException | KrbException e) {
                    eosDebugLogger.printStackTrace(e);
                }
                hasKrbTGT -= 1;
            }
        } else if (hasKrbTGT != 0 && yarnExecutor == 0) {        // we either have a Krb TGT or don't know yet
            try {
                setUgiKrbTokenCache();
            } catch (IOException | KrbException e) {
                eosDebugLogger.printStackTrace(e);
            }
        } else if (yarnExecutor == 1) {
            try {
                krb5ccname = getUgiKrbTokenCache();
            } catch (IOException | KrbException e) {
                eosDebugLogger.printStackTrace(e);
            }
            hasKrbTGT -= 1;
        }

        eosDebugLogger.printDebug("setKrb() "
                + "hasUgiKrbToken " + hasUgiKrbToken + "(" + hadUgiKrbToken + ") "
                + "hasKrbTGT " + hasKrbTGT + "(" + hadKrbTGT + ") "
                + "hasKrbST " + hasKrbST + "(" + hadKrbST + ") "
                + " krb5ccname: " + krb5ccname);

        return krb5ccname;
    }

    /**
     * Checks if KRB cache exists in the token cache
     */
    private static void checkToken() {
        // Check if we have UGI tokens
        try {
            boolean found = false;
            ugi = UserGroupInformation.getLoginUser();

            for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
                if (t.getKind().toString().equals(TOKEN_KRB_KIND)) {
                    found = true;
                    yarnExecutor = 1;
                }

                if (t.getKind().toString().equals(TOKEN_YARN_KIND)) {
                    yarnExecutor = 1;
                }

                if (yarnExecutor == 1) {
                    eosDebugLogger.printDebug("found UGI token " + t.getKind().toString() + "on the executor");
                } else {
                    eosDebugLogger.printDebug("found UGI token " + t.getKind().toString());
                }
            }

            if (found) {
                hasUgiKrbToken = 1;
            }
        } catch (IOException e) {
            eosDebugLogger.printWarn("Could not initialize UGI");
        }

        // Check if we have service tokens in credential cache
        // FIXME: This could be separate function checkCredentialCache which checks credential file cache, for TGT and TGS
        try {
            CredentialsCache ncc = CredentialsCache.getInstance();
            if (ncc != null) {
                hasKrbST = 0;
                for (Credentials cred: ncc.getCredsList()) {
                    if (cred.getServicePrincipal().getName().startsWith(TOKEN_PRINCIPAL_XROOTD_PREFIX)) {
                        String credCacheName = ncc.cacheName();
                        if (credCacheName != null) {
                            // Save cache location
                            XRootDKrb5.krb5ccname = credCacheName;
                            hasKrbST = 1;
                            eosDebugLogger.printDebug("found service ticket token " + cred.getServicePrincipal().getName() + " " + cred.getEndTime().toString() + " at " + credCacheName);
                            break;
                        }
                    }
                }
            } else {
                eosDebugLogger.printDebug("found no service ticket token");
            }
        } catch (RealmException e) {
            eosDebugLogger.printStackTrace(e);
        }
    }

    /**
     * Save Kerberos TGT in Token cache. This is used e.g. to propagate TGT to YARN applications with MapReduce
     */
    private static void setUgiKrbTokenCache() throws IOException, KrbException {
        if (hasUgiKrbToken > 0) {
            // Already set, perhaps because this is the M/R task
            return;
        }
        PrincipalName client = null;
        Credentials cccreds = null;
        boolean writeCache = false;

        int cc_version = FileCCacheConstants.KRB5_FCC_FVNO_3;

        String krb5ccname = null;

        // Try credential filecache to get TGT
        try {
            eosDebugLogger.printDebug("Reading credentials from Credentials Cache");
            CredentialsCache ncc = CredentialsCache.getInstance();
            if (ncc == null) {
                throw new IOException("Found no valid credentials cache");
            }

            krb5ccname = ncc.cacheName();
            cccreds = ncc.getDefaultCreds();
            if (cccreds == null && hasKrbST < 1) {
                throw new IOException("No valid Kerberos TGT or ST in credentials cache");
            }
            client = ncc.getPrimaryPrincipal();
        } catch (IOException e) {
            eosDebugLogger.printStackTrace(e);
        }

        // If not in credential filecache, try UGI credentials cache
        if (cccreds == null && ugi.hasKerberosCredentials()) {
            try {
                eosDebugLogger.printDebug("setKrbToken: Reading credentials from UGI");
                Method getTGT = ugi.getClass().getDeclaredMethod("getTGT");
                getTGT.setAccessible(true);
                KerberosTicket TGT = (KerberosTicket) getTGT.invoke(ugi);
                eosDebugLogger.printDebug("got TGT for " + ugi);

                KerberosPrincipal p = TGT.getClient();
                client = new PrincipalName(p.getName(), p.getNameType());
                cccreds = new Credentials(client, new PrincipalName(TGT.getServer().toString()), new EncryptionKey(TGT.getSessionKeyType(), TGT.getSessionKey().getEncoded()),
                        new KerberosTime(TGT.getAuthTime()), new KerberosTime(TGT.getStartTime()), new KerberosTime(TGT.getEndTime()), new KerberosTime(TGT.getRenewTill()),
                        false, new TicketFlags(TGT.getFlags()), null, null, new Ticket(TGT.getEncoded()), null);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                eosDebugLogger.printStackTrace(e);
            }
        }

        // If credential cache tgt not found, throw exception
        if (cccreds == null && hasKrbST < 1) {
            throw new IOException("No valid Kerberos TGT in credentials cache");
        } else if (cccreds == null && hasKrbST > 0) {
            // When there is service ticket, do not throw exception, but return gently
            eosDebugLogger.printDebug("No valid Kerberos TGT found in credentials cache, but found service ticket");
            return;
        }

        // If credential filecache not set, set default location
        if (krb5ccname == null) {
            writeCache = true;
            krb5ccname = System.getenv("KRB5CCNAME");
            if (krb5ccname != null && krb5ccname.length() > 5 && krb5ccname.regionMatches(true, 0, "FILE:", 0, 5)) {
                krb5ccname = krb5ccname.substring(5);
                eosDebugLogger.printDebug("Will write krb5ccname filename " + krb5ccname);
            } else {
                krb5ccname = Files.createTempFile("krb5", null).toString();
                eosDebugLogger.printDebug("Will write to newly created krb5ccname " + krb5ccname);
            }
        }

        // Save cache location
        XRootDKrb5.krb5ccname = krb5ccname;

        // Prepare token cache to be saved in UGI
        ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
        CCacheOutputStream ccos = new CCacheOutputStream(bos);

        // written: version, client_principal, default_creds
        ccos.writeHeader(client, cc_version);
        ccos.addCreds(cccreds);
        ccos.close();

        byte[] krb5cc = bos.toByteArray();
        bos.reset();

        Krb5TokenIdentifier k5id = new Krb5TokenIdentifier();
        DataOutputStream dos = new DataOutputStream(bos);
        k5id.write(dos);
        dos.close();

        eosDebugLogger.printDebug("setKrbToken saving krb5 ticket l=" + krb5cc.length + " in identifier l=" + bos.toByteArray().length);
        Token<? extends TokenIdentifier> t = new Token<Krb5TokenIdentifier>(bos.toByteArray(), krb5cc, new Text(TOKEN_KRB_KIND), new Text("Cerberus service"));
        if (writeCache ||
                (yarnExecutor == 1 && hasUgiKrbToken <= 0 && hasKrbTGT >= 0)) { //we want to set up krb renewal job in case there was only TGT (no token)
            try {
                eosDebugLogger.printDebug("Renewing token");
                t.renew(new Configuration());
            } catch (Exception e) {
                eosDebugLogger.printDebug("setKrbToken failed to renew " + t.toString() + ": " + e);
            }
        } else
            eosDebugLogger.printDebug("setKrbToken will not write/renew krb5 executor:" + yarnExecutor + ", hasKrbToken:" + hasUgiKrbToken + ",hasKrbTGT:" + hasKrbTGT);


        // Add token cache to UGI
        hasKrbTGT = 1;
        if (!ugi.addToken(t)) {
            hasUgiKrbToken = 0;
            eosDebugLogger.printDebug("setKrbToken failed to add token " + t.toString());
            throw new KrbException("could not add token " + t.toString());
        } else {
            hasUgiKrbToken = 1;
        }
    }

    /**
     *  Recover TGT from Token cache and set up krb5 credentials cache
     */
    private static String getUgiKrbTokenCache() throws IOException, KrbException {
        if (hasKrbTGT >= 0) {
            hasKrbTGT++;
            return krb5ccname;
        }

        boolean localTGTexists = false;
        String krb5ccname = (XRootDKrb5.krb5ccname.equals("")) ? XRootDFileSystem.getenv("KRB5CCNAME") : XRootDKrb5.krb5ccname;

        if (krb5ccname != null && krb5ccname.length() > 5) {
            if (krb5ccname.regionMatches(true, 0, "FILE:", 0, 5)) {
                krb5ccname = krb5ccname.substring(5);
            }
            eosDebugLogger.printDebug("krb5ccname filename " + krb5ccname);

            // if file exists we do not need to extract TGT from token
            if ((new File(krb5ccname)).exists()) {
                localTGTexists = true;
            } else
                eosDebugLogger.printDebug("krb5ccname points to " + krb5ccname + " but it does not seem to be valid");

        } else {
            krb5ccname = Files.createTempFile("krb5", null).toString();
            eosDebugLogger.printDebug("created krb5ccname " + krb5ccname);
        }
        // store the future location of TGT
        XRootDKrb5.krb5ccname = krb5ccname;

        Token<? extends TokenIdentifier> tok = null;

        for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
            if (t.getKind().toString().equals(TOKEN_KRB_KIND)) {
                tok = t;
                eosDebugLogger.printDebug("setKrbTGT found " + t);
                break;
            }
        }
        hasUgiKrbToken = (tok == null) ? 0 : 1;
        if (!localTGTexists) {
            if (hasUgiKrbToken == 0) {
                eosDebugLogger.printDebug("setKrbTGT: no valid Krb Token found");
                hasKrbTGT = 0;
                // cannot find a source for providing TGT
                throw new KrbException("setKrbTGT: no valid Krb Token");
            }
        }

        if (hasUgiKrbToken == 1) {

            eosDebugLogger.printDebug("setKrbTGT: Krb Token found in a token cache, will use it");

            Configuration conf = new Configuration();

            /* explicit renewal instead of "if (tok.isManaged()) tok.renew()": need the krb5ccname */
            Krb5TokenRenewer renewer = new Krb5TokenRenewer();
            long lifeTime = renewer.renew(tok, conf);

            eosDebugLogger.printDebug("setKrbTGT lifeTime " + lifeTime + " cache " + krb5ccname);
        } else {
            eosDebugLogger.printDebug("setKrbTGT: Not token found, but local TGT exists, will use it");
            hasKrbTGT = 1;
            //TBD: cannot write to filecahce
            //setKrbToken(); //set up token and renewal process
        }
        hasKrbTGT = 1;
        return krb5ccname;
    }
}
