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
package org.cloudifysource.dsl.cloud;

import java.io.File;
import java.net.UnknownHostException;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 */
public class CloudParsingTest {

	private static final String TEST_NETWORK_PARSING = "src/test/resources/clouds/network/basic";

	/**
	 * 
	 * @throws DSLException .
	 * @throws UnknownHostException .
	 */
	@Test
	public void testCloudWithNetworkParsing() throws DSLException, UnknownHostException {
		final File testDebugPath = new File(TEST_NETWORK_PARSING);

		final Cloud cloud = ServiceReader.readCloudFromDirectory(testDebugPath);
		Assert.assertNotNull(cloud);
		Assert.assertNotNull(cloud.getCloudNetwork());
		Assert.assertNotNull(cloud.getCloudNetwork().getManagement());
		Assert.assertNotNull(cloud.getCloudNetwork().getManagement().getNetworkConfiguration());
		Assert.assertNotNull(cloud.getCloudNetwork().getManagement().getNetworkConfiguration().getName());
		Assert.assertNotNull(cloud.getCloudNetwork().getManagement().getNetworkConfiguration().getSubnets());
		Assert.assertEquals(2, cloud.getCloudNetwork().getManagement().getNetworkConfiguration().getSubnets().size());
		Assert.assertEquals(2, cloud.getCloudCompute().getTemplates().get("SMALL_LINUX").getComputeNetwork().getNetworks().size());
		
		Assert.assertEquals("10.5.5.1", cloud.getCloudNetwork().getManagement().getNetworkConfiguration().getSubnets().get(0).getOptions().get("gateway"));
		Assert.assertEquals("10.5.5.2", cloud.getCloudNetwork().getManagement().getNetworkConfiguration().getSubnets().get(1).getOptions().get("gateway"));
		
		Assert.assertEquals(2, cloud.getCloudNetwork().getTemplates().get("APPLICATION_NET").getSubnets().size());
		Assert.assertEquals(3, cloud.getCloudNetwork().getTemplates().get("APPLICATION_NET2").getSubnets().size());
	}

}
