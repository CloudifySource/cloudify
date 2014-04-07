/*******************************************************************************
 * Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.ComputeLimits;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Flavor;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Limits;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Quota;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Router;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupRule;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.ServerFlavor;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackCloudifyDriverValidationTest {

	private class ValidationContextStub implements ValidationContext {

		@Override
		public void validationEvent(final ValidationMessageType messageType, final String message) {
		}

		@Override
		public void validationOngoingEvent(final ValidationMessageType messageType, final String message) {
		}

		@Override
		public void validationEventEnd(final ValidationResultType validtionResultType) {
		}
	}

	private OpenStackComputeClient computeApi;
	private OpenStackNetworkClient networkApi;

	@Before
	public void before() throws OpenstackException {
		computeApi = Mockito.mock(OpenStackComputeClient.class, Mockito.RETURNS_MOCKS);
		networkApi = Mockito.mock(OpenStackNetworkClient.class, Mockito.RETURNS_MOCKS);

		List<Network> createNetworks = this.createNetworks("SOME_INTERNAL_NETWORK_1", "SOME_INTERNAL_NETWORK_2");
		Mockito.when(networkApi.getNetworks()).thenReturn(createNetworks);
	}

	public OpenStackCloudifyDriver newDriverInstance(final String prefixCloudName, final boolean isManagement)
			throws Exception {
		File dslFile = new File("./src/test/resources/openstack/validations/" + prefixCloudName + "-cloud.groovy");
		Cloud cloud = ServiceReader.readCloud(dslFile);

		ComputeDriverConfiguration configuration = new ComputeDriverConfiguration();
		configuration.setCloud(cloud);
		configuration.setManagement(isManagement);
		configuration.setCloudTemplate(isManagement ? "MANAGER" : "APPLI");
		if (!isManagement) {
			configuration.setServiceName("default.test");
		}

		OpenStackCloudifyDriver driver = new OpenStackCloudifyDriver();
		driver.setConfig(configuration);
		driver.setComputeApi(computeApi);
		driver.setNetworkApi(networkApi);
		return driver;
	}

	private List<Network> createNetworks(final String... names) {
		List<Network> networks = new ArrayList<Network>(names.length);
		for (String name : names) {
			Network net = new Network();
			net.setName(name);
			networks.add(net);
		}
		return networks;
	}

	@Test
	public void testOKTemplate() throws Exception {
		try {

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Validation should not fail: " + e.getMessage());
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingOpenstackEndpoint() throws Exception {
		String openstackEndPoint = "openstack.endpoint";
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingOpenstackEndpoint", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains(String.format("The openstack endpoint '%s' is missing", openstackEndPoint))) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void wrongCredentials() throws Exception {

		try {
			Mockito.when(this.computeApi.getServers()).thenThrow(new OpenstackServerException(200, 401, null));
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("wrongCredentials", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (Exception e) {
			if (!e.getMessage().contains("Authentification operation failed. Please check credentials informations")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingManagementNetworkName() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementNetworkName", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains(
					"The name of Management network is missing. Please check management network configuration")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingManagementSubnetName() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementSubnetName", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("The name of subnet is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}

	}

	@Test(expected = CloudProvisioningException.class)
	public void missingManagementSubnetRange() throws Exception {
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementSubnetRange", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("The range is missing in subnet")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingApplicationNetworkName() throws Exception {
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationNetworkName", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("The name of template network configuration is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingApplicationSubnetName() throws Exception {
		try {

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationSubnetName", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("The name of the subnet is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingApplicationSubnetRange() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationSubnetRange", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("The range is missing in subnet")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingNetworksForManagements() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingNetworksForManagements", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("A network must be provided to the management machines")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingNetworksForApplications() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingNetworksForApplications", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("network must be provided for all templates: [APPLI, APPLI2]")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void wrongImageIdFormat() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("wrongFormatImageId", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("'imageId' should be formatted as region/imageId")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void wrongHardwareIdFormat() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("wrongFormatHardwareId", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("'hardwareId' should be formatted as region/flavorId")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void wrongImageIdResource() throws Exception {

		try {
			Mockito.reset(this.computeApi);
			Mockito.when(this.computeApi.getImage(Mockito.anyString())).thenThrow(new OpenstackException());

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("Image ID \"imageId\" is invalid")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void wrongHardwareIdResource() throws Exception {

		try {
			Mockito.reset(this.computeApi);
			Mockito.when(this.computeApi.getFlavor(Mockito.anyString())).thenThrow(new OpenstackException());

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("Hardware ID \"hardwareId\" is invalid")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingStaticSecurityGroup() throws Exception {

		try {
			Mockito.when(networkApi.getSecurityGroups()).thenThrow(new OpenstackException());

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("wrongSecurityGroops", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("Error requesting security groups")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingStaticNetwork() throws Exception {
		try {
			Mockito.reset(this.networkApi);
			List<Network> createNetworks = this.createNetworks("SOME_INTERNAL_NETWORK_2");
			Mockito.when(networkApi.getNetworks()).thenReturn(createNetworks);

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (CloudProvisioningException e) {
			if (!e.getMessage().contains("Network \"SOME_INTERNAL_NETWORK_1\" does not exist")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void instanceQuotaValidator() throws Exception {
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// Setting server list to return on 'computeApi.getServers()'
			final List<NovaServer> existingServers = new ArrayList<NovaServer>();
			final NovaServer server = new NovaServer();
			existingServers.add(server);
			Mockito.when(computeApi.getServers()).thenReturn(existingServers);
			
			// setting total instance limit to 1.
			final ComputeLimits computeLimits = new ComputeLimits();
			final Limits limits = new Limits();
			limits.setMaxTotalInstances(1);
			computeLimits.setLimits(limits);
			Mockito.when(computeApi.getLimits()).thenReturn(computeLimits);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("server instances quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void instanceCoreQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// Setting existing server list mock
			final List<NovaServer> existingServers = new ArrayList<NovaServer>();
			final NovaServer server = new NovaServer();
			server.setId("ServerID");
			existingServers.add(server);
			Mockito.when(computeApi.getServers()).thenReturn(existingServers);
			
			// setting server details mock to match flavor.
			final NovaServer serverDetails = new NovaServer();
			serverDetails.setId("ServerID");
			final ServerFlavor flavor = new ServerFlavor();
			flavor.setId("hardwareId");
			serverDetails.setFlavor(flavor);
			Mockito.when(computeApi.getServerDetails("ServerID")).thenReturn(serverDetails);
			
			// setting existing flavors list mock.
			final Flavor existingFlavor = new Flavor();
			existingFlavor.setId("hardwareId");
			existingFlavor.setVcpus(2);
			List<Flavor> existingFlavors = new ArrayList<Flavor>();
			existingFlavors.add(existingFlavor);
			Mockito.when(computeApi.getFlavors()).thenReturn(existingFlavors);
			
			// setting total core limit to 1.
			final ComputeLimits computeLimits = new ComputeLimits();
			final Limits limits = new Limits();
			limits.setMaxTotalCores(1);
			computeLimits.setLimits(limits);
			Mockito.when(computeApi.getLimits()).thenReturn(computeLimits);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("virtual CPUs quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void ramQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// Setting existing server list mock
			final List<NovaServer> existingServers = new ArrayList<NovaServer>();
			final NovaServer server = new NovaServer();
			server.setId("ServerID");
			existingServers.add(server);
			Mockito.when(computeApi.getServers()).thenReturn(existingServers);
			
			// setting server details mock to match flavor.
			final NovaServer serverDetails = new NovaServer();
			serverDetails.setId("ServerID");
			final ServerFlavor flavor = new ServerFlavor();
			flavor.setId("hardwareId");
			serverDetails.setFlavor(flavor);
			Mockito.when(computeApi.getServerDetails("ServerID")).thenReturn(serverDetails);
			
			// setting existing flavors list mock.
			final Flavor existingFlavor = new Flavor();
			existingFlavor.setId("hardwareId");
			existingFlavor.setRam(512);
			List<Flavor> existingFlavors = new ArrayList<Flavor>();
			existingFlavors.add(existingFlavor);
			Mockito.when(computeApi.getFlavors()).thenReturn(existingFlavors);
			
			// setting total ram limit to 512.
			final ComputeLimits computeLimits = new ComputeLimits();
			final Limits limits = new Limits();
			limits.setMaxTotalRAMSize(512);
			computeLimits.setLimits(limits);
			Mockito.when(computeApi.getLimits()).thenReturn(computeLimits);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("RAM quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void securityGroupQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// setting total security-group limit.
			final Quota quota = new Quota();
			quota.setSecurityGroup(20);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			// create 20 security-groups
			final List<SecurityGroup> existingSecurityGroups = new ArrayList<SecurityGroup>();
			for (int i = 0; i < 20; i++) {
				existingSecurityGroups.add(new SecurityGroup());
			} 
			Mockito.when(networkApi.getSecurityGroupsByTenantId("tenantId")).thenReturn(existingSecurityGroups);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("Security-groups quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void securityGroupRulesQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			final Quota quota = new Quota();
			// set security-group quota limit.
			quota.setSecurityGroupRule(20);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			// create 20 security-groups rules.
			final List<SecurityGroupRule> existingSecurityGroupRules = new ArrayList<SecurityGroupRule>();
			for (int i = 0; i < 20; i++) {
				existingSecurityGroupRules.add(new SecurityGroupRule());
			} 
			Mockito.when(networkApi.getSecurityGroupRulesByTenantId("tenantId")).thenReturn(existingSecurityGroupRules);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("Security-group rules quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void routersQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// setting total routers limit.
			final Quota quota = new Quota();
			// set router limit
			quota.setRouter(1);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			
			// init the existing router list
			final List<Router> existingRouters = new ArrayList<Router>();
			existingRouters.add(new Router());
			
			Mockito.when(networkApi.getRoutersByTenantId("tenantId")).thenReturn(existingRouters);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("routers quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void networksQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// setting total networks quota.
			final Quota quota = new Quota();
			// set network limit
			quota.setNetwork(1);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			
			// init the existing network list
			final List<Network> existingNetworks = new ArrayList<Network>();
			existingNetworks.add(new Network());
			
			Mockito.when(networkApi.getNetworksByTenantId("tenantId")).thenReturn(existingNetworks);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("networks quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void subnetsQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// setting total subnet quota.
			final Quota quota = new Quota();
			// set subnet limit
			quota.setSubnet(1);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			
			// init the existing subnet list
			final List<Subnet> existingSubnets = new ArrayList<Subnet>();
			existingSubnets.add(new Subnet());
			
			Mockito.when(networkApi.getSubnetsByTenantId("tenantId")).thenReturn(existingSubnets);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("subnets quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
	
	@Test(expected = CloudProvisioningException.class)
	public void floatingIpQuotaValidator() throws Exception {
		try {
			final OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
			
			// setting total floating-ips quota.
			final Quota quota = new Quota();
			// set floating-ip limit
			quota.setFloatingip(1);
			// set tenant mocking
			Mockito.when(computeApi.getTenantId()).thenReturn("tenantId");
			// set quotas mocking
			Mockito.when(networkApi.getQuotasForTenant("tenantId")).thenReturn(quota);
			
			// init the existing floating-ip list
			final List<FloatingIp> existingFloatingIps = new ArrayList<FloatingIp>();
			existingFloatingIps.add(new FloatingIp());
			
			Mockito.when(networkApi.getFloatingIps()).thenReturn(existingFloatingIps);
			
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (final CloudProvisioningException e) {
			if (!e.getMessage().contains("floating IPs quota limit exceeded")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}
}
