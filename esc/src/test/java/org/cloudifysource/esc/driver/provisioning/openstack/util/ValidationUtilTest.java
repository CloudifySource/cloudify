package org.cloudifysource.esc.driver.provisioning.openstack.util;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link ValidationUtil}
 * 
 * @author bouanane
 * 
 */
public class ValidationUtilTest {

	@Test(expected = IllegalStateException.class)
	public void missingSubnetNameTest() throws DSLException, CloudProvisioningException {

		final Cloud cloud =
				ServiceReader.readCloudFromDirectory("src/test/resources/openstack/openstack-no-subnet-name");
		final ComputeDriverConfiguration configuration = new ComputeDriverConfiguration();

		configuration.setCloud(cloud);
		ValidationUtil.validateAllNetworkNames(configuration);
		Assert.fail("IllegalStateException should be thrown, since subnet name is missing");

	}
}
