/* FileSystem wrapper for EOS and Castor "xrootd" based file systems
 *
 * Author: Rainer Toebbicke, CERN IT
 */

package ch.cern.eos;

import java.lang.System;
import java.lang.reflect.Field;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

/*from java.io import ByteArrayInputStream, ByteArrayOutputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream*/

import java.util.Arrays;

import java.lang.UnsatisfiedLinkError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.BufferedFSInputStream;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;

import org.apache.hadoop.util.Progressable;

import sun.security.krb5.KrbException;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ccache.Credentials;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.ccache.CCacheInputStream;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.ccache.FileCredentialsCache;
import sun.security.krb5.PrincipalName;

import ch.cern.eos.EOSInputStream;
import ch.cern.eos.Krb5TokenIdentifier;

public class EOSFileSystem extends FileSystem {
    private long nHandle = 0;
    private static boolean libLoaded = false;
    private native long initFileSystem(String url);
    private native FileStatus getFileStatusS(long nHandle, String fn, Path p);
    private native FileStatus[] listFileStatusS(long nHandle, String fn, Path p);
    private native long Mv(long nHandle, String src, String dest);
    private native long Rm(long nHandle, String fn);
    private native long MkDir(long nHandle, String fn, short mode);
    private native long  RmDir(long nHandle, String fn);
    private native static void setcc(String ccname);
    public native String getErrText(long errcode);

    private static final Log LOG = LogFactory.getLog(EOSFileSystem.class);
    private URI uri;

    public static boolean EOS_debug = false;
    public static int hasKrbToken = -1;
    public static int hasKrbTGT = -1;
    public static int buffer_size = 10*1024*1024;

    public EOSFileSystem() {
    }

    public URI toUri(Path p) throws IOException {
	try {
	    if (p.getName().indexOf('?') < 0)
		return p.toUri();
	    URI u = new URI(p.toString());	    // need to re-parse otherwise '?query' becomes part of filename
	    if (EOS_debug) System.out.println("EOSFileSystem.toUri (Scheme,Authority,Path,Query): " + u.getScheme() + "," + u.getAuthority() + "," + u.getPath() + "," + u.getQuery());
	    return u;
	} catch (URISyntaxException e) {
	    if (EOS_debug) e.printStackTrace();
	    throw new IOException("Invalid URI Syntax");
	}
    }

    public int FileStatus(long length, boolean isdir, int block_replication,
	                         long blocksize, long modification_time, Path path) { int xx=42; return xx;}

    public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) {
	throw new IllegalArgumentException("append");
    }

    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
	if (nHandle == 0) initHandle();
	URI u = toUri(f);
	String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
	// String filespec = u.getPath();
	if (u.getQuery() != null) filespec += "?" + u.getQuery();

	if (EOS_debug) {
	    System.out.println("EOSfs create " + u.getScheme() + "://" + u.getAuthority() + u.getPath() + " --> " + filespec);
	}
	return new FSDataOutputStream(new EOSOutputStream(filespec, permission, overwrite), null);
    }

    public boolean delete(Path p, boolean recursive) throws IOException {
	if (nHandle == 0) initHandle();
	
	long status = 0;
	String filespec = toUri(p).getPath();

	FileStatus std = getFileStatusS(nHandle, filespec, p);

	if (std.isDirectory()) {
	    if (recursive) {
		if (EOS_debug) System.out.println("EOSFileSystem.delete recursive " + filespec);
		FileStatus st[] = listStatus(p);
		for (FileStatus s: st) {
		    if (s.isDirectory()) {
			if (!delete(s.getPath(), recursive)) break;
		    } else { 
			status = Rm(nHandle, s.getPath().toUri().getPath());
			if (EOS_debug) System.out.println("EOSFileSystem.delete " + s.getPath().toString() + " status = " + status);
			if (status != 0) break;
		    }
		}
	    }
	    if (status == 0) {
		status = RmDir(nHandle, filespec);
		if (EOS_debug) System.out.println("EOSFileSystem.delete RmDir " + filespec + " status = " + status);
	    }
	} else {
	    status = Rm(nHandle, filespec);
	    if (EOS_debug) System.out.println("EOSFileSystem.delete " + filespec + " status = " + status);
	}
	if (status != 0) throw new IOException("Cannot delete " + p.toString() + ", status = " + status);

	return status == 0;
    }

    public static void initLib() throws IOException {
	if (System.getenv("EOS_debug") != null) {
	    EOS_debug = true;
	}
	if (libLoaded) return;

	String jlp = System.getProperty("java.library.path");
	if (!jlp.contains("/usr/lib/hadoop/lib/native")) {
	    System.setProperty("java.library.path", "/usr/lib/hadoop/lib/native:" + jlp);
	    if(EOS_debug) System.out.println("EOSfs.initlib: using java.library.path: " + System.getProperty("java.library.path"));
	    //found by googling... not that I understood it...
	    //set sys_paths to null so that java.library.path will be reevaluated next time it is needed
	    try {
		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
	    } catch (Exception e) {
		System.out.println("Could not reset java.library.path: " + e.getMessage());
		e.printStackTrace();
	    }
	}

	try {
	    System.loadLibrary("jXrdCl");
	} catch (UnsatisfiedLinkError e) {
	    e.printStackTrace();
	    System.out.println("failed to load jXrdCl, java.library.path=" + jlp + ", 'root' scheme disabled");		/* very likely only says "/usr/lib/hadoop/lib/native" */
	    throw new IOException();
	}

    }

    private static void checkToken() {
	UserGroupInformation ugi;

	try {
	    ugi = UserGroupInformation.getLoginUser();
	} catch (IOException e) {
	    System.out.println("IOException in checkToken");
	    return;
	}

	for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
	    if (Arrays.equals(t.getIdentifier(), "krb5cc".getBytes())) {
		hasKrbToken = 1;
		return;
	    }
	}

	/* either no Krb token, or called too early? Leave it at current (limbo) state */
	return;
    }


    private void initHandle() throws IOException {
	if (nHandle != 0) return;
	initLib();

	String fsStr = uri.getScheme() + "://" + uri.getAuthority();
	nHandle = initFileSystem(uri.getScheme() + "://" + uri.getAuthority());
	if (EOS_debug) System.out.println("initFileSystem(" + fsStr + ") = " + nHandle);

        String ccname =  System.getenv("KRB5CCNAME");

        //check if there is a valid TGT
	 validateKrbTGT();
	

	if (hasKrbToken < 0 && hasKrbTGT < 0) setKrb();
    }
   


    private void validateKrbTGT()
    {
       //checking if there is valid TGT under KRB5CCNAME
	String ccname =  System.getenv("KRB5CCNAME");
//        CredentialsCache ncc;
//	sun.security.krb5.Credentials crn;

        if (ccname == null ) return;
	if (ccname.length() > 5 && ccname.regionMatches(true, 0,  "FILE:", 0, 5))
	{
               ccname = ccname.substring(5);
	}
	else return;


        try
	{
//		crn = sun.security.krb5.Credentials.acquireDefaultCreds().renew();		
//	        ncc = CredentialsCache.create(crn.getClient(), ccname);
	}
	catch (Exception e)
	{
	    return;
	}
        if (hasKrbTGT!=1)
            setcc("KRB5CCNAME=FILE:" + ccname);
        hasKrbTGT=1;
        

    }
   

    public static void setKrb() {
	try {
	    initLib();		// needed mostly to get EOS_debug set
	} catch (IOException e) {
	}

	/* if no Krb ticket, set from Token. If no Krb Token, set from ticket */

	int hadKrbTGT = hasKrbTGT, hadKrbToken = hasKrbToken;

	if (hasKrbToken < 0 && hasKrbTGT < 0) checkToken();

	if (hasKrbToken > 0) {
	    try {
		setKrbTGT();
	    } catch(IOException | KrbException e) {
	    }
	} else if (hasKrbTGT != 0) {
	    try {
		setKrbToken();
	    } catch(IOException | KrbException e) {
	    }
	}

	if (EOS_debug) {
	    System.out.println("setKrb: hasKrbToken " + hasKrbToken + "(" + hadKrbToken + ") hasKrbTGT " + hasKrbTGT + "(" + hadKrbTGT + ")"); /* */
	}
    }


    public static void setkrbcc(String ccname) throws IOException {
	initLib();
	setcc("KRB5CCNAME=FILE:" + ccname);
    }

    /* Save Kerberos TGT in Token cache so it gets propagated to map/reduce jobs */
    public static void setKrbToken() throws IOException, KrbException {
	
	if (hasKrbToken > 0) return;	    /* Already set, perhaps because this is the M/R task */


	sun.security.krb5.Credentials crn;

	/* need to retrieve the KRB ticket in any case, hence just try instead of testing whether already done */
	try {
	    crn = sun.security.krb5.Credentials.acquireDefaultCreds().renew();
	    /* System.out.println("Krb renew ok");		/* */
	    hasKrbTGT = 1;
	} catch (UnsatisfiedLinkError | KrbException e) {
	    /* hasKrbTGT = 0;	    /* leave it untouched for now */
	    throw new KrbException("No valid Kerberos ticket");
	}

	Credentials cccreds = new Credentials(crn.getClient(), crn.getServer(), crn.getSessionKey(),
			 new KerberosTime(crn.getAuthTime()), new KerberosTime(crn.getStartTime()), new KerberosTime(crn.getEndTime()), new KerberosTime(crn.getRenewTill()),
			 false, crn.getTicketFlags(), null, crn.getAuthzData(), crn.getTicket(), null);
	String krb5ccname = System.getenv("KRB5CCNAME");
	if (EOS_debug) System.out.println("setKrbToken krb5ccname " + krb5ccname);
	CredentialsCache ncc;
	if (krb5ccname == null) {
	    krb5ccname = Files.createTempFile("krb5", null).toString();
	    ncc = CredentialsCache.create(crn.getClient(), krb5ccname);
	} else {	/* reuse session's crdentials cache */
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
    public static String setKrbTGT() throws IOException, KrbException {
	// UserGroupInformation ugi = UserGroupInformation.getLoginUser();
	UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

	String krb5ccname = "";
	Token<? extends TokenIdentifier> tok = null;

	for (Token<? extends TokenIdentifier> t : ugi.getTokens()) {
	    /* System.out.println("setKrbTGT: token " + t.toString());	    /* */
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

	sun.security.krb5.Credentials crn = FileCredentialsCache.acquireInstance(pp, krb5ccname).getDefaultCreds().setKrbCreds().renew();
	Credentials cccreds = new Credentials(crn.getClient(), crn.getServer(), crn.getSessionKey(),
		 new KerberosTime(crn.getAuthTime()), new KerberosTime(crn.getStartTime()), new KerberosTime(crn.getEndTime()), new KerberosTime(crn.getRenewTill()),
		 false, crn.getTicketFlags(), null, crn.getAuthzData(), crn.getTicket(), null);
	CredentialsCache ncc = CredentialsCache.create(crn.getClient(), krb5ccname);
	ncc.update(cccreds);
	ncc.save();

	setkrbcc(krb5ccname);
	hasKrbTGT = 1;

	return(krb5ccname);

    };

    public void initialize(URI uri, Configuration conf) throws IOException {
	setConf(conf);

	// System.out.println("EOS initialize: EOS_debug " + EOS_debug + " uri scheme " + uri.getScheme() + " authority " + uri.getAuthority() + " path " + uri.getPath() + " query " + uri.getQuery());

	this.uri = uri;

        initLib();

        validateKrbTGT();
        
        if (hasKrbToken < 0 && hasKrbTGT < 0) 
	   setKrb();
    }

    public String getScheme() {
	return "root";
    }

    public static URI getDefaultURI(Configuration conf) {
	return URI.create("root://");
    }

    public FSDataInputStream open(Path path, int bufSize) throws IOException {
	if (nHandle == 0) initHandle();
	URI u = toUri(path);
	String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
	// String filespec = u.getPath();
	if (EOS_debug) {
	    System.out.println("EOSfs open " + u.getScheme() + "://" + u.getAuthority() + u.getPath() + " --> " + filespec);
	}
	return new FSDataInputStream(new BufferedFSInputStream (new EOSInputStream(filespec),buffer_size));
    }


    public FileStatus getFileStatus(Path p) throws IOException {
	if (nHandle == 0) initHandle();

	FileStatus st = getFileStatusS(nHandle, toUri(p).getPath(), p);
	if (st == null) {
	    throw new FileNotFoundException("File not found");
	}

	if (EOS_debug) System.out.println(st.toString());

	return st;
    }

    public FileStatus[] listStatus(Path p) throws IOException {
	if (nHandle == 0) initHandle();
	URI u = toUri(p);
	// String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
	String filespec = u.getPath();
	if (u.getQuery() != null) filespec += "?" + u.getQuery();
	return listFileStatusS(nHandle, filespec, p);
    }

    public boolean mkdirs(Path p, FsPermission permission) throws IOException {
	if (nHandle == 0) initHandle();
	URI u = toUri(p);
	// String filespec = uri.getScheme() + "://" +  uri.getAuthority() + "/" + u.getPath();
	String filespec = u.getPath();
	if (u.getQuery() != null) 
	    filespec += "?" + u.getQuery();
	long st = MkDir(nHandle, filespec, permission.toShort());
	return st == 0;
    }

    public URI getUri() {
	return uri;
    }

    public Path getWorkingDirectory() {
	return new Path(uri);
    }

    public void setWorkingDirectory(Path f) {
	throw new IllegalArgumentException("setWorkingDirectory");
    }

    public boolean rename(Path src, Path dst) throws IOException {
	if (nHandle == 0) initHandle();
	long st = Mv(nHandle, toUri(src).getPath(), toUri(dst).getPath());
	return st == 0;
    };

};

