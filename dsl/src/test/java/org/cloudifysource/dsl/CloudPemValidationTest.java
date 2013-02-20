/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/**
 * @author noak
 * This test validates a missing pem file error throws the correct error message.
 */
public class CloudPemValidationTest {

	private static final String EC2_MISSING_PEM_PATH = "testResources/testparsing/ec2-missing-pem-cloud.groovy";
	
	/**
	 * Parses the given cloud groovy file, which point to a fake pem file.
	 * The missing pem file should cause a dsl-validation error, with a proper message.
	 */
	@Test
	public void testCloudParser() {
		try {
			ServiceReader.readCloud(new File(EC2_MISSING_PEM_PATH));
			//if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The key file is not found yet no error was thrown", false);
		} catch (Throwable e) {
			assertTrue("The key file is not found yet no error was thrown. Error was: " + e.getMessage(), 
					e.getMessage().contains("The specified key file is missing") 
					|| e.getMessage().contains("The specified key file was not found"));
		}
	}


}
