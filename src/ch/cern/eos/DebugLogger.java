/* 
 * Author: CERN IT
 */
package ch.cern.eos;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DebugLogger {

    private Logger logger = LogManager.getRootLogger();
    
    public DebugLogger(boolean debugEnabled) {
        this.setDebug(debugEnabled);
    }

    public void print(String e) {
    	this.logger.info(e);
    }
    
    public void printDebug(String e) {
    	this.logger.debug(e);
    }

    public void printStackTrace(Exception e) {
        if (this.logger.isDebugEnabled()) {
        	this.logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void setDebug(boolean debugEnabled) {
	this.logger.setLevel(debugEnabled ? Level.DEBUG : Level.INFO);
    }
    
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }
}
