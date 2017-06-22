package ch.cern.eos;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;

import org.apache.hadoop.io.Text;

 
import ch.cern.eos.Krb5TokenIdentifier;

import sun.security.krb5.KrbException;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ccache.Credentials;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.ccache.CCacheInputStream;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.ccache.FileCredentialsCache;
import sun.security.krb5.PrincipalName;

import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Arrays;




public class EOSKrb5
{
  
      public static String krb5ccname="";
      public static int hasKrbToken = -1;
      public static int hasKrbTGT = -1;
      public static boolean EOS_debug=false;

   
      public static String setKrb() {
       // try {
     //       initLib();          // needed mostly to get EOS_debug set
        //} catch (IOException e) {
        //}

        /* if no Krb ticket, set from Token. If no Krb Token, set from ticket */

        int hadKrbTGT = hasKrbTGT, hadKrbToken = hasKrbToken;

        if (hasKrbToken < 0 && hasKrbTGT < 0)
        {
           //nothing initialized

           //check local KRB cache
           if(checkTGT())
              hasKrbTGT=1;
           

           if (hasKrbTGT!=1)
              if(checkToken()) //check token cache
                 hasKrbToken=1;
           
        }



        if (hasKrbToken > 0) {
         //if has a token try to initialize Krb cahe
            try {
                setKrbTGT();
            } 
            catch(IOException | KrbException e) {
            }

        } 
        else if (hasKrbTGT > 0) 
        {
	 //if TGT exists try to insert it into token cache
            try {
                setKrbToken();
            } 
            catch(IOException | KrbException | NullPointerException e) {
            }
        }

        if (EOS_debug) {
            System.out.println("setKrb: hasKrbToken " + hasKrbToken + "(" + hadKrbToken + ") hasKrbTGT " + hasKrbTGT + "(" + hadKrbTGT + ")"); /* */
        }
        return krb5ccname;
    }
    
    private static boolean checkTGT()
    {
        String ccname =  System.getenv("KRB5CCNAME");

        CredentialsCache ncc;
        sun.security.krb5.Credentials crn;

        if (ccname == null ) return false;
        if (ccname.length() > 5 && ccname.regionMatches(true, 0,  "FILE:", 0, 5))
        {
               ccname = ccname.substring(5);
        }
        else 
	   return false;


        try
        {
	      //testing if the krb cache is valid
              crn = sun.security.krb5.Credentials.acquireDefaultCreds().renew();
              ncc = CredentialsCache.create(crn.getClient(), ccname);
        }
        catch (Exception e)
        {
            return false;
        }
        krb5ccname=ccname;
        return true;



    }

    //Checks if KRB cache exists in the token cache
    private static boolean checkToken() {

        UserGroupInformation ugi;

        try {
            ugi = UserGroupInformation.getLoginUser();
        } catch (IOException e) {
            System.out.println("IOException in checkToken");
            return false;
        }

        for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
            if (Arrays.equals(t.getIdentifier(), "krb5cc".getBytes())) {
                return true;
            }
        }

        /* either no Krb token, or called too early? Leave it at current (limbo) state */
        return false;
    }

    
    /* Save Kerberos TGT in Token cache so it gets propagated to YARN applications */
    public static void setKrbToken() throws IOException, KrbException {

        if (hasKrbToken > 0) return;        /* Already set, perhaps because this is the M/R task */


        sun.security.krb5.Credentials crn;

        /* need to retrieve the KRB ticket in any case, hence just try instead of testing whether already done */
        try {
            crn = sun.security.krb5.Credentials.acquireDefaultCreds().renew();
            /* System.out.println("Krb renew ok");              /* */
            hasKrbTGT = 1;
        } catch (UnsatisfiedLinkError | KrbException e) {
            /* hasKrbTGT = 0;       /* leave it untouched for now */
            throw new KrbException("No valid Kerberos ticket");
        }

        Credentials cccreds = new Credentials(crn.getClient(), crn.getServer(), crn.getSessionKey(),
                         new KerberosTime(crn.getAuthTime()), new KerberosTime(crn.getStartTime()), new KerberosTime(crn.getEndTime()), new KerberosTime(crn.getRenewTill()),
                         false, crn.getTicketFlags(), null, crn.getAuthzData(), crn.getTicket(), null);
        //String krb5ccname = System.getenv("KRB5CCNAME");
        if (EOS_debug) System.out.println("setKrbToken krb5ccname " + krb5ccname);
        CredentialsCache ncc;
        if (krb5ccname == null) {
            krb5ccname = Files.createTempFile("krb5", null).toString();
            ncc = CredentialsCache.create(crn.getClient(), krb5ccname);
        } else {        /* reuse session's crdentials cache */
            if (krb5ccname.length() > 5 && krb5ccname.regionMatches(true, 0,  "FILE:", 0, 5))
               krb5ccname = krb5ccname.substring(5);
            /* ncc = (CredentialsCache) FileCredentialsCache.acquireInstance(crn.getClient(), krb5ccname); /* */
            ncc = CredentialsCache.create(crn.getClient(), krb5ccname);

            if (EOS_debug) System.out.println("setKrbToken cacheName " + ncc.cacheName());
        }
        ncc.update(cccreds);
        if (EOS_debug) System.out.println("setKrbToken update ok " + ncc.cacheName());
        ncc.save();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        CCacheOutputStream ccos = new CCacheOutputStream(bos);
        ccos.writeHeader(crn.getClient(), ((FileCredentialsCache) ncc).version);
        ccos.addCreds(ncc.getDefaultCreds());
        ccos.close();
        byte [] krb5cc = bos.toByteArray();

        Token<? extends TokenIdentifier> t = new Token<Krb5TokenIdentifier>("krb5cc".getBytes(), krb5cc, new Text("krb5"), new Text("Cerberus service"));
        UserGroupInformation ugi = UserGroupInformation.getLoginUser();
        if (!ugi.addToken(t)) {
            hasKrbToken = 0;
            System.out.println("Failed to add token " + t.toString());
            throw new KrbException("could not add token " + t.toString());
        } else {
            hasKrbToken = 1;
        }
    }

    /* Recover TGT from Token cache and set up krb5 crdentials cache */
    public static void setKrbTGT() throws IOException, KrbException {
        // UserGroupInformation ugi = UserGroupInformation.getLoginUser();
        UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

        String krb5ccname = "";
        Token<? extends TokenIdentifier> tok = null;

        for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
            /* System.out.println("setKrbTGT: token " + t.toString());      /* */
            if (Arrays.equals(t.getIdentifier(), "krb5cc".getBytes())) {
                tok = t;
                break;
            }
        }

        if (tok == null) {
            hasKrbToken = 0;
            throw new KrbException("No valid Krb Token");
        }


        byte krb5cc[] = krb5cc = tok.getPassword();

        krb5ccname = Files.createTempFile("krb5_", null).toString();
        CCacheInputStream ccis = new CCacheInputStream(new ByteArrayInputStream(krb5cc));
        int version = ccis.readVersion();
        PrincipalName pp = ccis.readPrincipal(version);
        /* cccred = ccis.readCred(version) does not work alas, not a public method */
        ccis.close();
        FileOutputStream fos = new FileOutputStream(krb5ccname);
        fos.write(krb5cc);
        fos.close();

        synchronized(this) {
            sun.security.krb5.Credentials crn = FileCredentialsCache.acquireInstance(pp, krb5ccname).getDefaultCreds().setKrbCreds().renew();
            Credentials cccreds = new Credentials(crn.getClient(), crn.getServer(), crn.getSessionKey(),
                     new KerberosTime(crn.getAuthTime()), new KerberosTime(crn.getStartTime()), new KerberosTime(crn.getEndTime()), new KerberosTime(crn.getRenewTill()),
                     false, crn.getTicketFlags(), null, crn.getAuthzData(), crn.getTicket(), null);
            CredentialsCache ncc = CredentialsCache.create(crn.getClient(), krb5ccname);
            ncc.update(cccreds);
            ncc.save();
        }

     //   setkrbcc(krb5ccname);
        hasKrbTGT = 1;
        EOSKrb5.krb5ccname=krb5ccname;

        //return krb5ccname;

    };



 
    
}

