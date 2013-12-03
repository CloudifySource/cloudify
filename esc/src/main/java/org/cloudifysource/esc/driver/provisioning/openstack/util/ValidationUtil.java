package org.cloudifysource.esc.driver.provisioning.openstack.util;

import java.util.Collection;
import java.util.List;

import org.cloudifysource.domain.cloud.network.ManagementNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;

/**
 * Class for names validation
 * 
 * @author bouanane
 * 
 */
public class ValidationUtil {

	private ValidationUtil() {
	}

	/**
	 * Validates all subnet names (including management one)
	 * 
	 * @param configuration
	 *            configuration to process
	 * @throws CloudProvisioningException
	 *             If the subnet was not set or empty.
	 */
	public static void validateAllNetworkNames(ComputeDriverConfiguration configuration) throws IllegalStateException {

		ManagementNetwork mn = configuration.getCloud().getCloudNetwork().getManagement();
		validatMangementeNetworkName(mn);

		Collection<NetworkConfiguration> templates = configuration.getCloud().getCloudNetwork().getTemplates().values();
		if (templates != null && !templates.isEmpty()) {
			for (NetworkConfiguration nc : templates) {
				validateNetworkName(nc);
			}
		}
	}

	/**
	 * validates subnet (application) name
	 * 
	 * @param networkConfiguration
	 *            network configuration to process
	 * @throws IllegalStateException
	 *             If the subnet was not set or empty.
	 */
	private static void validateNetworkName(NetworkConfiguration networkConfiguration)
			throws IllegalStateException {

		if (networkConfiguration != null) {
			List<org.cloudifysource.domain.cloud.network.Subnet> subnets = networkConfiguration.getSubnets();
			if (subnets != null && !subnets.isEmpty()) {
				for (org.cloudifysource.domain.cloud.network.Subnet sn : subnets) {
					if (sn.getName() == null || sn.getName().trim().equals("")) {
						throw new IllegalStateException(
								"The name of the Subnet must be provided, Please check network block :'"
										+ networkConfiguration.getName() + "'");
					}
				}
			}
		}
	}

	/**
	 * validates management subnet name
	 * 
	 * @param managementNetwork
	 *            network configuration to process
	 * @throws IllegalStateException
	 *             If the subnet was not set or empty.
	 */
	private static void validatMangementeNetworkName(ManagementNetwork managementNetwork) throws IllegalStateException {

		if (managementNetwork != null) {
			validateNetworkName(managementNetwork.getNetworkConfiguration());
		}
	}
}
