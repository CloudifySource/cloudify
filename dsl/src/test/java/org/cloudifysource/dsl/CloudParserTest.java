/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Assert;
import org.junit.Test;

public class CloudParserTest {

	private final static String SIMPLE_CLOUD_PATH = "src/test/resources/enums/my-cloud.groovy";

	@Test
	public void testCloudParser() throws Exception {
		final org.cloudifysource.dsl.cloud.Cloud cloud = ServiceReader.readCloud(new File(SIMPLE_CLOUD_PATH));
		assertNotNull(cloud);
		assertNotNull(cloud.getProvider());
		assertNotNull(cloud.getTemplates());
		assertNotNull(cloud.getUser());
		assertNotNull(cloud.getTemplates().size() == 1);
		assertNotNull(cloud.getTemplates().get("SMALL_LINUX"));
		Assert.assertEquals(FileTransferModes.CIFS, cloud.getTemplates().get("SMALL_LINUX").getFileTransfer());

	}

}
