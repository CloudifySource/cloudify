package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

	public OpenStackCloudifyDriver newDriverInstance(String prefixCloudName, boolean isManagement)
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

	private List<Network> createNetworks(String... names) {
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
		Assert.fail("To be implemented");
	}

	@Test
	public void wrongCredentials() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void missingManagementNetworkName() throws Exception {
		Assert.fail("To be implemented");

	}

	@Test
	public void missingManagementSubnetName() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void missingManagementSubnetRange() throws Exception {
		Assert.fail("To be implemented");
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
		Assert.fail("To be implemented");
	}

	@Test
	public void missingNetworksForManagements() throws Exception {
		Assert.fail("To be implemented");

	}

	@Test
	public void missingNetworksForApplications() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void wrongImageIdFormat() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void wrongHardwareIdFormat() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void wrongImageIdResource() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void wrongHardwareIdResource() throws Exception {
		Assert.fail("To be implemented");
	}

	@Test
	public void missingStaticSecurityGroup() throws Exception {
		Assert.fail("To be implemented");
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
