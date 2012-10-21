/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
