/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.byon;

import groovy.lang.GString;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.byon.CustomNodeImpl;
import org.cloudifysource.esc.driver.provisioning.context.DefaultProvisioningDriverClassContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class tests the parsing of different ID/IP configurations in byoun-cloud.groovy.
 *
 * @author noak
 *
 */
public class ParseByonCloudNodesTest {

	private static final String PROVIDER = "BYON";
	private static final String CLOUD_NODES_LIST = "nodesList";
	private static final String TEST_RESOURCES = "testResources/byon";

	@Test
	public void testByonWithGstring() throws IOException, DSLException, CloudProvisioningException {
		final File cloudFile = new File("src/test/resources/byon-gstring/testbyon-cloud.groovy");
		final Cloud cloud = ServiceReader.readCloud(cloudFile);

		validateCloud(cloud);

		ByonProvisioningDriver driver = new ByonProvisioningDriver();
		driver.setProvisioningDriverClassContext(new DefaultProvisioningDriverClassContext());
		driver.setConfig(cloud, cloud.getCloudCompute().getTemplates().keySet().iterator().next(), true, "test");

		Cloud modifiedCloud = driver.getCloud();

		// make sure there were no modifications to the cloud object
		validateCloud(modifiedCloud);

		ByonDeployer deployer = driver.getDeployer();

		Set<CustomNode> nodes = deployer.getAllNodesByTemplateName(cloud.getCloudCompute().
				getTemplates().keySet().iterator().next());
		Assert.assertNotNull(nodes);
		Assert.assertEquals(1, nodes.size());
		CustomNode node = nodes.iterator().next();
		Assert.assertEquals("pc-lab100", node.getPrivateIP());



	}

	private void validateCloud(final Cloud cloud) {
		Assert.assertNotNull(cloud);
		Assert.assertTrue(cloud.getCloudCompute().getTemplates().size() == 1);
		ComputeTemplate template = cloud.getCloudCompute().getTemplates().values().iterator().next();
		Assert.assertNotNull(template.getCustom());

		List<Object> list = (List<Object>) template.getCustom().get("nodesList");

		Assert.assertTrue(list.size() == 1);

		Map<Object, Object> map = (Map<Object, Object>) list.iterator().next();
		Assert.assertTrue(map.containsKey("host-list"));
		final Object hostList = map.get("host-list");

		Assert.assertTrue(hostList instanceof GString);
	}

	/**
	 * Test parsing.
	 */
	@Test
	public void test() {
		try {
			// load the cloud file
			final File cloudFile = new File(TEST_RESOURCES + "/testbyon-cloud.groovy");
			System.out.println("Trying to read cloud file " + cloudFile.getAbsolutePath());
			final Cloud cloud = ServiceReader.readCloud(cloudFile);

			System.out.println("Creating BYON deployer");
			final ByonDeployer deployer = new ByonDeployer();
			List<Map<String, String>> nodesList = null;
			final Map<String, ComputeTemplate> templatesMap = cloud.getCloudCompute().getTemplates();
			for (final String templateName : templatesMap.keySet()) {
				final Map<String, Object> customSettings = cloud.getCloudCompute().getTemplates().get(templateName).getCustom();
				Assert.assertNotNull("Custom settings not found for template " + templateName, customSettings);
				if (customSettings != null) {
					nodesList = (List<Map<String, String>>) customSettings.get(CLOUD_NODES_LIST);
				}
				Assert.assertNotNull("NodesList not found for template " + templateName, nodesList);
				if (nodesList == null) {
					System.out.println("Failed to create cloud deployer, invalid configuration, nodesList is null");
					throw new CloudProvisioningException("Failed to create BYON cloud deployer, invalid configuration");
				}
				deployer.addNodesList(templateName, templatesMap.get(templateName), nodesList);
			}

			Set<CustomNode> allNodes = deployer.getAllNodesByTemplateName("SMALL_LINUX");
			Set<CustomNode> expectedNodes = new HashSet<CustomNode>();
			// id, ip, username, credential
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test01", "0.0.0.1", "tgrid1", "tgrid1",
					"byon-test01"));
			// id, ip
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test02", "0.0.0.2", null, null, "byon-test02"));
			// idPrefix, ipList
			// expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test11", "0.0.0.3", null, null, "byon-test11"));
			// expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test12", "0.0.0.4", null, null, "byon-test12"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test11", "pc-lab39", null, null, "byon-test11"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test12", "pc-lab40", null, null, "byon-test12"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test13", "0.0.0.5", null, null, "byon-test13"));
			// id (template), ipList
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test21", "0.0.0.6", null, null, "byon-test21"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test22", "0.0.0.7", null, null, "byon-test22"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test23", "0.0.0.8", null, null, "byon-test23"));
			// id, ipRange
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test31", "0.0.0.9", null, null, "byon-test31"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test32", "0.0.0.10", null, null, "byon-test32"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test33", "0.0.0.11", null, null, "byon-test33"));
			// id, CIDR
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test41", "0.0.0.12", null, null, "byon-test41"));
			expectedNodes.add(new CustomNodeImpl(PROVIDER, "byon-test42", "0.0.0.13", null, null, "byon-test42"));
			System.out.println(Arrays.toString(allNodes.toArray()));

			Assert.assertTrue("Wrong output", allNodes.size() == expectedNodes.size()
					&& expectedNodes.containsAll(allNodes) && allNodes.containsAll(expectedNodes));
		} catch (final Exception e) {
			System.out.println("Failed to create cloud deployer, exception thrown: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalStateException("Failed to create cloud deployer", e);
		}
	}

}
