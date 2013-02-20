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

public class CloudMissingTemplateTest {

	private  static final String EC2_MISSING_TEMPLATE_PATH =
			"testResources/testparsing/ec2-missing-template-cloud.groovy";

	@Test
	public void testCloudParser()
			throws Exception {
		try {
			final org.cloudifysource.dsl.cloud.Cloud cloud =
					ServiceReader.readCloud(new File(EC2_MISSING_TEMPLATE_PATH));
			// if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The management template does not exist yet no error was thrown", false);
		} catch (final Throwable e) {
			assertTrue("The management template does not exist yet no error was thrown. Error was: " + e.getMessage(),
					e.getMessage().contains("is not listed in the cloud's templates section")
							|| e.getMessage().contains("managementMachineTemplate"));
		}
	}

}
