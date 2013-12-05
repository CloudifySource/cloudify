/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.openstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.domain.cloud.network.Subnet;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OpenStackNetworkConfigurationHelperTest {

	private static final Object MNG_COMPUTE_TEMPLATE = "MANAGER";
	private static final String APPLI_COMPUTE_TEMPLATE = "APPLI";
	private static final String TEMPLATE_MNG_NAME = "Cloudify-Management-Network";
	private static final String TEMPLATE_APPLI_NAME = "Cloudify-Application-Network";
	private static final String COMPUTE_NET1 = "SOME_INTERNAL_NETWORK_1";
	private static final String COMPUTE_NET2 = "SOME_INTERNAL_NETWORK_2";

	private static final String APPLICATION_NET = "APPLICATION_NET";

	private Cloud cloud;
	private CloudNetwork cloudNetwork;
	private List<String> mngComputeNetwork;
	private List<String> appliComputeNetwork;
	private NetworkConfiguration mngNetConfig;
	private NetworkConfiguration appliNetConfig;

	@Before
	public void before() throws DSLException {
		cloud = ServiceReader.readCloudFromDirectory("src/test/resources/openstack/networks-configuration");
		cloudNetwork = cloud.getCloudNetwork();
		Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
		mngComputeNetwork = templates.get(MNG_COMPUTE_TEMPLATE).getComputeNetwork().getNetworks();
		appliComputeNetwork = templates.get(APPLI_COMPUTE_TEMPLATE).getComputeNetwork().getNetworks();
		mngNetConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
		appliNetConfig = cloud.getCloudNetwork().getTemplates().get(APPLICATION_NET);
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// **** Network names validations
	// ****************************************************************************************
	// ****************************************************************************************
	@Test
	public void testValidateNetworkNames() {
		try {
			new OpenStackNetworkConfigurationHelper().validateNetworkNames(cloudNetwork);
			new OpenStackNetworkConfigurationHelper().validateNetworkNames(new CloudNetwork());
		} catch (CloudProvisioningException e) {
			Assert.fail("Test should pass but got " + e.getMessage());
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingManagementNetworkNameTest() throws CloudProvisioningException {
		cloudNetwork.getManagement().getNetworkConfiguration().setName(null);
		new OpenStackNetworkConfigurationHelper().validateNetworkNames(cloudNetwork);
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingManagementSubnetNameTest() throws CloudProvisioningException {
		cloudNetwork.getManagement().getNetworkConfiguration().getSubnets().get(0).setName(null);
		new OpenStackNetworkConfigurationHelper().validateNetworkNames(cloudNetwork);
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingApplicationNetworkNameTest() throws CloudProvisioningException {
		cloudNetwork.getTemplates().get(APPLICATION_NET).setName(null);
		new OpenStackNetworkConfigurationHelper().validateNetworkNames(cloudNetwork);
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingApplicationSubnetNameTest() throws CloudProvisioningException {
		cloudNetwork.getTemplates().get(APPLICATION_NET).getSubnets().get(0).setName(null);
		new OpenStackNetworkConfigurationHelper().validateNetworkNames(cloudNetwork);
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// **** Network configuration tests
	// ****************************************************************************************
	// ****************************************************************************************

	private void removeManagementNetworkConfiguration() {
		cloud.getCloudNetwork().getManagement().getNetworkConfiguration().setName(null);
		cloud.getCloudNetwork().getManagement().getNetworkConfiguration().setSubnets(new ArrayList<Subnet>());
	}

	private ComputeDriverConfiguration createConfiguration(final Cloud cloud, final String serviceNetworkTemplate,
			final boolean isManagement) {

		ComputeDriverConfiguration configuration = new ComputeDriverConfiguration();
		configuration.setCloud(cloud);
		configuration.setManagement(isManagement);

		if (!isManagement) {
			configuration.setCloudTemplate(APPLI_COMPUTE_TEMPLATE);
		}

		if (serviceNetworkTemplate != null) {
			ServiceNetwork network = new ServiceNetwork();
			network.setTemplate(serviceNetworkTemplate);
			configuration.setNetwork(network);
		}

		return configuration;
	}

	/**
	 * MANAGEMENT
	 * 
	 * <ul>
	 * <li>With management network.</li>
	 * </ul>
	 */
	@Test
	public void testManagementNetworkTemplates() throws CloudProvisioningException {
		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, true);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(mngNetConfig, helper.getNetworkConfiguration());
		Assert.assertEquals(mngComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(null, helper.getApplicationNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(true, helper.useServiceNetworkTemplate());
		Assert.assertEquals(true, helper.useManagementNetwork());
	}

	/**
	 * MANAGEMENT
	 * 
	 * <ul>
	 * <li>No management network.</li>
	 * </ul>
	 */
	@Test
	public void testManagementComputeTemplates() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, true);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(null, helper.getNetworkConfiguration());
		Assert.assertEquals(mngComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(null, helper.getApplicationNetworkName());
		Assert.assertEquals(null, helper.getManagementNetworkName());
		Assert.assertEquals(COMPUTE_NET1, helper.getPrivateIpNetworkName());

		Assert.assertEquals(false, helper.useServiceNetworkTemplate());
		Assert.assertEquals(false, helper.useManagementNetwork());
	}

	@Test
	public void testManagementNoNetworkAtAll() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		cloud.getCloudCompute().getTemplates().get(MNG_COMPUTE_TEMPLATE).getComputeNetwork()
				.setNetworks(new ArrayList<String>());
		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, true);

		try {
			new OpenStackNetworkConfigurationHelper(configuration);
		} catch (CloudProvisioningException e) {
			Assert.assertTrue("Expected CloudProvisioningException",
					e.getMessage().contains("A network must be provided to the management machines"));
		}
	}

	/**
	 * APPLI
	 * 
	 * <ul>
	 * <li>Network template specified in recipe.</li>
	 * </ul>
	 */
	@Test
	public void testAppliNetworkTemplates() throws CloudProvisioningException {
		ComputeDriverConfiguration configuration = createConfiguration(cloud, APPLICATION_NET, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(appliNetConfig, helper.getNetworkConfiguration());
		Assert.assertEquals(appliComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getApplicationNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(true, helper.useServiceNetworkTemplate());
		Assert.assertEquals(true, helper.useManagementNetwork());
	}

	/**
	 * APPLI
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>There is a computeNetwork.</li>
	 * </ul>
	 */
	@Test
	public void testAppliComputeTemplates() throws CloudProvisioningException {
		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(null, helper.getNetworkConfiguration());
		Assert.assertEquals(appliComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(COMPUTE_NET2, helper.getApplicationNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(false, helper.useServiceNetworkTemplate());
		Assert.assertEquals(true, helper.useManagementNetwork());
	}

	/**
	 * APPLI
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>No computeNetwork.</li>
	 * </ul>
	 */
	@Test
	public void testAppliFirstTemplateNetwork() throws CloudProvisioningException {
		cloud.getCloudCompute().getTemplates().get(APPLI_COMPUTE_TEMPLATE).getComputeNetwork()
				.setNetworks(new ArrayList<String>());

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(appliNetConfig, helper.getNetworkConfiguration());
		Assert.assertTrue(helper.getComputeNetworks().isEmpty());

		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getApplicationNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(true, helper.useServiceNetworkTemplate());
		Assert.assertEquals(true, helper.useManagementNetwork());
	}

	/**
	 * APPLI
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>No computeNetwork.</li>
	 * <li>No network templates.</li>
	 * </ul>
	 */
	@Test
	public void testAppliNoNetworksAtAll() throws CloudProvisioningException {

		cloud.getCloudNetwork().setTemplates(new HashMap<String, NetworkConfiguration>());
		cloud.getCloudCompute().getTemplates().get(APPLI_COMPUTE_TEMPLATE).getComputeNetwork()
				.setNetworks(new ArrayList<String>());

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(mngNetConfig, helper.getNetworkConfiguration());
		Assert.assertTrue(helper.getComputeNetworks().isEmpty());

		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getApplicationNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_MNG_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(false, helper.useServiceNetworkTemplate());
		Assert.assertEquals(true, helper.useManagementNetwork());
	}

	// *****************************
	// *****************************

	/**
	 * APPLI without management network
	 * 
	 * <ul>
	 * <li>Network template specified in recipe.</li>
	 * </ul>
	 */
	@Test
	public void testAppliNetworkTemplatesNoManagementNetwork() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		ComputeDriverConfiguration configuration = createConfiguration(cloud, APPLICATION_NET, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(appliNetConfig, helper.getNetworkConfiguration());
		Assert.assertEquals(appliComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getApplicationNetworkName());
		Assert.assertEquals(null, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(true, helper.useServiceNetworkTemplate());
		Assert.assertEquals(false, helper.useManagementNetwork());
	}

	/**
	 * APPLI without management network
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>There is a computeNetwork.</li>
	 * </ul>
	 */
	@Test
	public void testAppliComputeTemplatesNoManagementNetwork() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(null, helper.getNetworkConfiguration());
		Assert.assertEquals(appliComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(COMPUTE_NET2, helper.getApplicationNetworkName());
		Assert.assertEquals(null, helper.getManagementNetworkName());
		Assert.assertEquals(COMPUTE_NET2, helper.getPrivateIpNetworkName());

		Assert.assertEquals(false, helper.useServiceNetworkTemplate());
		Assert.assertEquals(false, helper.useManagementNetwork());
	}

	/**
	 * APPLI without management network
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>No computeNetwork.</li>
	 * </ul>
	 */
	@Test
	public void testAppliFirstTemplateNetworkNoManagementNetwork() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		cloud.getCloudCompute().getTemplates().get(APPLI_COMPUTE_TEMPLATE).getComputeNetwork()
				.setNetworks(new ArrayList<String>());

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(appliNetConfig, helper.getNetworkConfiguration());
		Assert.assertTrue(helper.getComputeNetworks().isEmpty());

		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getApplicationNetworkName());
		Assert.assertEquals(null, helper.getManagementNetworkName());
		Assert.assertEquals(TEMPLATE_APPLI_NAME, helper.getPrivateIpNetworkName());

		Assert.assertEquals(true, helper.useServiceNetworkTemplate());
		Assert.assertEquals(false, helper.useManagementNetwork());
	}

	/**
	 * APPLI without management network
	 * 
	 * <ul>
	 * <li>No network template specified in recipe.</li>
	 * <li>No computeNetwork.</li>
	 * <li>No network templates.</li>
	 * </ul>
	 */
	@Test
	public void testAppliNoNetworksAtAllNoManagementNetwork() throws CloudProvisioningException {

		this.removeManagementNetworkConfiguration();

		cloud.getCloudNetwork().setTemplates(new HashMap<String, NetworkConfiguration>());
		cloud.getCloudCompute().getTemplates().get(APPLI_COMPUTE_TEMPLATE).getComputeNetwork()
				.setNetworks(new ArrayList<String>());

		ComputeDriverConfiguration configuration = createConfiguration(cloud, null, false);
		OpenStackNetworkConfigurationHelper helper = new OpenStackNetworkConfigurationHelper(configuration);

		Assert.assertEquals(null, helper.getNetworkConfiguration());
		Assert.assertEquals(mngComputeNetwork, helper.getComputeNetworks());

		Assert.assertEquals(COMPUTE_NET1, helper.getApplicationNetworkName());
		Assert.assertEquals(null, helper.getManagementNetworkName());
		Assert.assertEquals(COMPUTE_NET1, helper.getPrivateIpNetworkName());

		Assert.assertEquals(false, helper.useServiceNetworkTemplate());
		Assert.assertEquals(false, helper.useManagementNetwork());
	}

}
