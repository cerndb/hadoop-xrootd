/* 
 * Author: CERN IT
 */
package ch.cern.eos;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class EOSDebugLogger {

    private Logger logger = LogManager.getLogger();
    
    public EOSDebugLogger(boolean debugEnabled) {
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
        Configurator.setRootLevel(debugEnabled ? Level.DEBUG : Level.INFO);
    }
    
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }
}