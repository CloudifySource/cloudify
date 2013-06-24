/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.doclet;

import java.io.File;

import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.generation.RestDoclet;
import org.junit.Test;

/**
 * A class to test the rest-doclet.
 * @author yael
 *
 */
public class RestDocletTest {
	private static final String DOCLET_DIR_PATH = 
			"src" + File.separator + "test" + File.separator + "resources" + File.separator + "restDoclet";
	private static final String DOC_DEST_PATH = DOCLET_DIR_PATH + File.separator + "restdoclet.html";
	private static final String DOC_CSS_PATH = DOCLET_DIR_PATH + File.separator + "restdoclet.css";
	private static final String VELOCITY_TEMPLATE_PATH = 
			DOCLET_DIR_PATH + File.separator 
			+ RestDocConstants.VELOCITY_TEMPLATE_FILE_NAME;
	private static final String SOURCES_PATH = "src" + File.separator + "main" + File.separator + "java";

	@Test
	public void test() {
		
		com.sun.tools.javadoc.Main.execute(new String[] {
				RestDocConstants.DOCLET_FLAG, RestDoclet.class.getName(),
				RestDocConstants.SOURCE_PATH_FLAG, SOURCES_PATH, RestDocConstants.CONTROLLERS_PACKAGE,
				RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG, VELOCITY_TEMPLATE_PATH,
				RestDocConstants.DOC_DEST_PATH_FLAG, DOC_DEST_PATH, 
				RestDocConstants.DOC_CSS_PATH_FLAG, DOC_CSS_PATH,
				RestDocConstants.VERSION_FLAG, RestDocConstants.VERSION,
				RestDocConstants.REQUEST_EXAMPLE_GENERATOR_CLASS_FLAG, RESTRequestExampleGenerator.class.getName(),
				RestDocConstants.RESPONSE_EXAMPLE_GENERATOR_CLASS_FLAG, RESTResposneExampleGenerator.class.getName(),
				RestDocConstants.REQUEST_BODY_PARAM_FILTER_CLASS_FLAG, RequestBodyParamFilter.class.getName()
				});

	}

}
