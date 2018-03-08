package ch.cern.eos;

import java.io.IOException;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;

public class XrootDBasedKerberizedFileSystem extends XrootDBasedFileSystem {

	public XrootDBasedKerberizedFileSystem() {
		super();
	}
	
	protected void initHandle() throws IOException {
		super.initHandle();
		setkrbcc(XrootDBasedKrb5.setKrb());
    }
	
	public void initialize(URI uri, Configuration conf) throws IOException {
		super.initialize(uri, conf);
		setkrbcc(XrootDBasedKrb5.setKrb());
    }

    /*
     * Setting token cache from TGT (on Spark or MR drivers) or init local krb cache from token 
     * (if mapper or executor) 
     */
    public static void setKrb() {
		XrootDBasedKrb5.setKrb();
    }

    /* 
     * This sets (setenv()) KRB5CCNAME in the current (!) environment, 
     * which is NOT the one java currently sees, nor the one a java sub-process is going to see spawned
     * using execve() - for the latter one would have to modify java's copy of the environment which is doable.
     * jython or scala may yet play different games
     */
    public static void setkrbcc(String ccname) throws IOException {
		XrootDBasedFileSystem.initLib();
		setenv("KRB5CCNAME", "FILE:" + ccname);
    }
}
