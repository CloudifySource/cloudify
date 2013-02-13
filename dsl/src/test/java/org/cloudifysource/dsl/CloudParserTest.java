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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Assert;
import org.junit.Test;

public class CloudParserTest {

	private static final String SIMPLE_CLOUD_PATH = "src/test/resources/enums/my-cloud.groovy";
	private static final String SIMPLE_BAD_CLOUD_PATH = "src/test/resources/enums/my-bad-cloud.groovy";
	private final static String INSTALLER_CLOUD_PATH = "src/test/resources/clouds/installer/some-cloud.groovy";

	@Test
	public void testCloudParser() throws Exception {
		final org.cloudifysource.dsl.cloud.Cloud cloud = ServiceReader.readCloud(new File(SIMPLE_CLOUD_PATH));
		assertNotNull(cloud);
		assertNotNull(cloud.getProvider());
		assertNotNull(cloud.getCloudCompute().getTemplates());
		assertNotNull(cloud.getUser());
		assertNotNull(cloud.getCloudCompute().getTemplates().size() == 1);
		assertNotNull(cloud.getCloudCompute().getTemplates().get("SMALL_LINUX"));
		Assert.assertEquals(FileTransferModes.CIFS, cloud.getCloudCompute()
				.getTemplates().get("SMALL_LINUX").getFileTransfer());

	}
	
	@Test
	public void testCloudParserWithTemplatesUnderCloudSection() throws Exception {
		try {
			ServiceReader.readCloud(new File(SIMPLE_BAD_CLOUD_PATH));
			Assert.fail("Cloud parsing should not be succesfull " 
					+ "since the templates are located under the cloud section");
		} catch (IllegalArgumentException e) {
			// this should throw this exception since 
			// we moved the templates to a compute section
		}
	}
	
	@Test
	public void testStorageTemplate() throws IOException, DSLException {
		final Cloud cloud = ServiceReader.readCloud(new File(SIMPLE_CLOUD_PATH));
		Assert.assertNotNull(cloud.getCloudStorage());
		Assert.assertNotNull(cloud.getCloudStorage().getTemplates());
		Assert.assertNotNull(cloud.getCloudStorage().getTemplates().get("SMALL_BLOCK"));
		Assert.assertEquals(5, cloud.getCloudStorage().getTemplates().get("SMALL_BLOCK").getSize());
	}


	@Test
	public void testCloudParserWithInstaller() throws Exception {
		final org.cloudifysource.dsl.cloud.Cloud cloud = ServiceReader.readCloud(new File(INSTALLER_CLOUD_PATH));
		assertNotNull(cloud);
		assertNotNull(cloud.getCloudCompute().getTemplates());
		assertNotNull(cloud.getCloudCompute().getTemplates().size() == 1);
		assertNotNull(cloud.getCloudCompute().getTemplates().get("SMALL_LINUX"));

		org.cloudifysource.dsl.cloud.ComputeTemplate template = cloud.getCloudCompute().getTemplates().values().iterator().next();
		assertNotNull(template);
		assertNotNull(template.getInstaller());

		CloudTemplateInstallerConfiguration installer = template.getInstaller();
		assertEquals(5000, installer.getConnectionTestConnectTimeoutMillis());
		assertEquals(5000, installer.getConnectionTestIntervalMillis());
		assertEquals(5, installer.getFileTransferRetries());


	}



}
