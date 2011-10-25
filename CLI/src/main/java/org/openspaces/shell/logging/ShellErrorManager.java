package org.openspaces.shell.logging;

import java.text.MessageFormat;
import java.util.logging.ErrorManager;

public class ShellErrorManager extends ErrorManager{
	

    /**
     * Constructs an instance of {@code ErrorManager}.
     */
    public ShellErrorManager() {
        super ();
    }
    
    @Override
    public void error(String message, Exception exception, int errorCode) {
   
        System.err.println(MessageFormat.format("Error occurred: {0}. " +
        		"For more details refer to the full logs.", exception.getCause()));
    
         exception.printStackTrace(System.err);
    }
}
