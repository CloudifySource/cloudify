package org.cloudifysource.restDoclet.generation;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.restDoclet.constants.RestDocConstants;

import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;


public class RestDoclet extends Doclet {
	private static final Logger logger = Logger.getLogger(RestDoclet.class.getName());

	public static boolean start(final RootDoc root) {
		try{
			new Generator(root).run();
			logger.log(Level.INFO, "REST API documentation was successfully generated.");
			return true;
		}
		catch(Exception e) {
			logger.log(Level.SEVERE, "Failed to generate REST API documentation: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public static int optionLength(String option) {
		if(RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG.equals(option)
				|| RestDocConstants.DOC_DEST_PATH_FLAG.equals(option)
				|| RestDocConstants.VERSION_FLAG.equals(option)) {
			return 2;
		}
		return 0;
	}
	
	

}
