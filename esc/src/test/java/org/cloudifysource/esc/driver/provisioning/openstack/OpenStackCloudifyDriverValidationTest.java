package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Image;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.expression.spel.ast.OpNE;

public class OpenStackCloudifyDriverValidationTest {

	private class ValidationContextStub implements ValidationContext {

		@Override
		public void validationEvent(ValidationMessageType messageType, String message) {
		}

		@Override
		public void validationOngoingEvent(ValidationMessageType messageType, String message) {
		}

		@Override
		public void validationEventEnd(ValidationResultType validtionResultType) {
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

	@Test
	public void missingOpenstackEndpoint() throws Exception {
		String openstackEndPoint = "openstack.endpoint";
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingOpenstackEndpoint", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains(String.format("The openstack endpoint '%s' is missing", openstackEndPoint))) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
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

	@Test
	public void missingManagementNetworkName() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementNetworkName", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains(
					"The name of Management network is missing. Please check management network configuration")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}

	@Test
	public void missingManagementSubnetName() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementSubnetName", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains("The Name of subnet is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}

	}

	@Test
	public void missingManagementSubnetRange() throws Exception {
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingManagementSubnetRange", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains("The range is missing in subnet")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}

	@Test
	public void missingApplicationNetworkName() throws Exception {
		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationNetworkName", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains("The name of template network configuration is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}

	@Test
	public void missingApplicationSubnetName() throws Exception {
		try {

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationSubnetName", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (Exception e) {
			if (!e.getMessage().contains("The name of the subnet is missing")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}

	@Test
	public void missingApplicationSubnetRange() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingApplicationSubnetRange", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains("The range is missing in subnet")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}

	@Test(expected = CloudProvisioningException.class)
	public void missingNetworksForManagements() throws Exception {

		try {
			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("missingNetworksForManagements", false);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());
		} catch (Exception e) {
			if (!e.getMessage().contains("has no networks for cloudify communications")) {
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
		} catch (Exception e) {
			if (!e.getMessage().contains("A network must be provided for all templates")) {
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

		} catch (Exception e) {
			if (!e.getMessage().contains("Error requesting security groups")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
			throw e;
		}
	}

	@Test
	public void missingStaticNetwork() throws Exception {
		try {
			Mockito.reset(this.networkApi);
			List<Network> createNetworks = this.createNetworks("SOME_INTERNAL_NETWORK_2");
			Mockito.when(networkApi.getNetworks()).thenReturn(createNetworks);

			OpenStackCloudifyDriver newDriverInstance = this.newDriverInstance("ok", true);
			newDriverInstance.validateCloudConfiguration(new ValidationContextStub());

		} catch (Exception e) {
			if (!e.getMessage().contains("Network \"SOME_INTERNAL_NETWORK_1\" does not exist")) {
				e.printStackTrace();
				Assert.fail("Validation must fail: " + e.getMessage());
			}
		}
	}
}
