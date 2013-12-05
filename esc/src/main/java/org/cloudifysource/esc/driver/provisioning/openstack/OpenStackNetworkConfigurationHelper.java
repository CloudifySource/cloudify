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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.compute.ComputeTemplateNetwork;
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.domain.cloud.network.ManagementNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.domain.network.AccessRules;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;

/**
 * This class handle network configuration priority and make life easier to the driver.<br />
 * The implementation is dedicated to the driver and should be instantiate only by the setConfig of the driver.<br />
 * <br />
 * <b>BEAR IN MIND that this class know if the driver is a manager instance or an service instance and return the
 * expected configuration.</b>
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackNetworkConfigurationHelper {

	private static final String ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP = "associateFloatingIpOnBootstrap";

	private boolean management;

	private NetworkConfiguration managementNetworkConfiguration;
	private List<String> managementComputeNetworks;
	private Map<String, Object> managementNetworkOptions;

	private NetworkConfiguration serviceNetworkConfiguration;
	private List<String> serviceComputeNetworks;
	private AccessRules serviceAccessRules;
	private boolean serviceUseNetworkTemplate;

	/**
	 * Constructor for testing purpose.
	 */
	OpenStackNetworkConfigurationHelper() {
	}

	public OpenStackNetworkConfigurationHelper(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {

		this.validateNetworkNames(configuration.getCloud().getCloudNetwork());
		this.initManagementNetworkConfig(configuration);
		this.management = configuration.isManagement();
		if (!this.management) {
			this.initServiceNetworkConfig(configuration);
		}

	}

	/**
	 * Initialize properties for management network configuration.<br />
	 * A network must be provided for management either it is configured in <code>cloudNetwork</code> template or in the
	 * <code>computeNetwork</code> of the cloudCompute.
	 * 
	 * @param configuration
	 *            The configuration.
	 * @throws CloudProvisioningException
	 *             Thrown if no networks defined for the management machine.
	 * 
	 */
	private void initManagementNetworkConfig(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {
		final Cloud cloud = configuration.getCloud();
		final String template = cloud.getConfiguration().getManagementMachineTemplate();

		// Init management computeNetworks. Check if there is any.
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(template);
		if (computeTemplate != null) {
			final ComputeTemplateNetwork computeNetwork = computeTemplate.getComputeNetwork();
			if (computeNetwork != null) {
				this.managementComputeNetworks = computeNetwork.getNetworks();
			}
			this.managementNetworkOptions = computeTemplate.getOptions();
		}

		if (managementComputeNetworks == null) {
			this.managementComputeNetworks = new ArrayList<String>();
		}

		// Figure out if there is a management network.
		final NetworkConfiguration mngConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
		if (mngConfig != null && mngConfig.getName() != null && mngConfig.getSubnets() != null
				&& !mngConfig.getSubnets().isEmpty()) {
			this.managementNetworkConfiguration = mngConfig;
		}

		if (this.managementNetworkConfiguration == null && managementComputeNetworks.isEmpty()) {
			throw new CloudProvisioningException(
					"A network must be provided to the management machines "
							+ "(use either cloudNetwork templates or computeNetwork configuration).");
		}
	}

	/**
	 * Initialize properties for service network configuration.<br />
	 * 
	 * @param configuration
	 *            The configuration.
	 * @throws CloudProvisioningException
	 *             Thrown if the service declared a network template which do not exist in the list of templates.
	 */
	private void initServiceNetworkConfig(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {

		// Init management computeNetworks. Check if there is any.
		final Map<String, ComputeTemplate> computeTemplates = configuration.getCloud().getCloudCompute().getTemplates();
		final ComputeTemplate computeTemplate = computeTemplates.get(configuration.getCloudTemplate());
		if (computeTemplate != null) {
			final ComputeTemplateNetwork computeNetwork = computeTemplate.getComputeNetwork();
			if (computeNetwork != null) {
				this.serviceComputeNetworks = computeNetwork.getNetworks();
			}
		}
		if (this.serviceComputeNetworks == null) {
			this.serviceComputeNetworks = new ArrayList<String>();
		}

		// Figure out the application network to use
		final ServiceNetwork serviceNetwork = configuration.getNetwork();
		if (serviceNetwork != null) {
			this.serviceAccessRules = serviceNetwork.getAccessRules();
		}
		final CloudNetwork cloudNetwork = configuration.getCloud().getCloudNetwork();

		if (cloudNetwork != null && serviceNetwork != null && serviceNetwork.getTemplate() != null) {
			// The service specified a network template to use.
			final Map<String, NetworkConfiguration> templates = cloudNetwork.getTemplates();
			this.serviceNetworkConfiguration = templates.get(serviceNetwork.getTemplate());
			this.serviceUseNetworkTemplate = true;
			if (this.serviceNetworkConfiguration == null) {
				throw new CloudProvisioningException("Service network template not found '"
						+ serviceNetwork.getTemplate() + "'");
			}
		} else if (this.serviceComputeNetworks.isEmpty()) {
			// The service did not specified a computeNetworks to use.
			// So try to get the first application network.
			if (cloudNetwork != null
					&& cloudNetwork.getTemplates() != null
					&& !cloudNetwork.getTemplates().isEmpty()) {
				// There is an application network template
				this.serviceUseNetworkTemplate = true;
				this.serviceNetworkConfiguration = cloudNetwork.getTemplates().values().iterator().next();
			} else {
				// if no application network template, use the management network.
				if (this.useManagementNetwork()) {
					// use the management network template if exists.
					this.serviceNetworkConfiguration = this.managementNetworkConfiguration;
				} else {
					// Or use the management computeNetwork
					this.serviceComputeNetworks = this.managementComputeNetworks;
				}
			}
		}
	}

	/**
	 * Get the network template.<br />
	 * This class know if the driver is a manager instance or an service instance and return the expected configuration.
	 * 
	 * @return The network template to use.
	 */
	public NetworkConfiguration getNetworkConfiguration() {
		return management ? this.managementNetworkConfiguration : this.serviceNetworkConfiguration;
	}

	/**
	 * Returns <true> if the service recipe uses network template otherwise returns <code>false</code>.
	 * 
	 * @return Returns <true> if the service recipe uses network template, returns <code>false</code> otherwise.
	 */
	public boolean useServiceNetworkTemplate() {
		if (management) {
			return this.useManagementNetwork();
		} else {
			return this.serviceUseNetworkTemplate;
		}
	}

	/**
	 * Returns <true> if the network configuration uses a management network, returns <code>false</code> otherwise.
	 * 
	 * @return Returns <true> if the network configuration uses a management network, returns <code>false</code>
	 *         otherwise.
	 */
	public boolean useManagementNetwork() {
		return this.getManagementNetworkName() != null;
	}

	/**
	 * Returns the management network name (defined in the configuration).
	 * 
	 * @return Returns the management network name.
	 */
	public String getManagementNetworkName() {
		if (this.managementNetworkConfiguration != null) {
			return managementNetworkConfiguration.getName();
		}
		return null;
	}

	/**
	 * <p>
	 * Returns the name of the private IP network to use.
	 * </p>
	 * <p>
	 * For the Cloudify managers, that would be :
	 * <ol>
	 * <li>The management network if exists.</li>
	 * <li>If the management network does not exist, it would be the first network in the computeNetwork list.</li>
	 * </ol>
	 * </p>
	 * <p>
	 * For the Cloudify agents, that would be :
	 * <ol>
	 * <li>The management network if exists.</li>
	 * <li>If none, the network defined in the service recipe.</li>
	 * <li>If none, the first network defined in the computeNetwork (from the cloudCompute of the service).</li>
	 * <li>If none, the first network in the network template.</li>
	 * <li>If none, the same private IP network as the management machines.</li>
	 * </ol>
	 * </p>
	 * 
	 * @return Returns the name of the private IP network to use.
	 */
	public String getPrivateIpNetworkName() {
		String name = null;

		if (this.useManagementNetwork()) {
			// If there is a management network then there it is
			name = managementNetworkConfiguration.getName();
		} else {
			if (this.management) {
				// If none of the below cases, then it would be the first network of computeNetworks.
				name = managementComputeNetworks.get(0);
			} else {
				if (this.serviceUseNetworkTemplate) {
					// If no management network but the service specified the network template network.
					// Then it would be the network in the template
					name = serviceNetworkConfiguration.getName();
				} else if (!serviceComputeNetworks.isEmpty()) {
					// If none then it would be the first network of computeNetworks.
					name = serviceComputeNetworks.get(0);
				} else if (this.isServiceAndManagementNotSameNetwork()) {
					// If none then it would be the first network of cloudNetwork template.
					name = this.serviceNetworkConfiguration.getName();
				} else {
					// If still none then use the same as the management;
					name = managementComputeNetworks.get(0);
				}
			}
		}
		return name;
	}

	/**
	 * Watch out !!! it tests the objects references !
	 */
	private boolean isServiceAndManagementNotSameNetwork() {
		return this.serviceNetworkConfiguration.getName() != this.managementNetworkConfiguration.getName();
	}

	/**
	 * Returns the application network name (only one).
	 * 
	 * <p>
	 * For the Cloudify managers, returns <code>null</code>.
	 * </p>
	 * 
	 * <p>
	 * For the Cloudify agents:
	 * <ol>
	 * <li>The network defined in the service recipe.</li>
	 * <li>If none, the first network defined in the computeNetwork (from the cloudCompute of the service).</li>
	 * <li>If none, the first network in the network template.</li>
	 * <li>If none, the same private IP network as the management machines.</li>
	 * </ol>
	 * </p>
	 * 
	 * @return Returns the application network name (only one).
	 */
	public String getApplicationNetworkName() {
		if (this.management) {
			return null;
		} else {
			if (this.serviceUseNetworkTemplate) {
				return this.getNetworkConfiguration().getName();
			} else {
				if (!this.getComputeNetworks().isEmpty()) {
					return this.getComputeNetworks().get(0);
				} else if (this.isServiceAndManagementNotSameNetwork()) {
					return this.serviceNetworkConfiguration.getName();
				} else if (this.useManagementNetwork()) {
					return this.managementNetworkConfiguration.getName();
				} else {
					return managementComputeNetworks.get(0);
				}
			}
		}
	}

	/**
	 * Returns the network names defined in the computeNetworks.
	 * 
	 * @return Returns the network names defined in the computeNetworks.
	 */
	public List<String> getComputeNetworks() {
		return management ? this.managementComputeNetworks : this.serviceComputeNetworks;
	}

	public AccessRules getServiceAccessRules() {
		return serviceAccessRules;
	}

	/**
	 * Verify the all networks and subnets has a name (including management one).
	 * 
	 * @param cloudNetwork
	 *            Configuration to process.
	 * @throws CloudProvisioningException
	 *             If a network or subnet is missing name.
	 */
	protected void validateNetworkNames(final CloudNetwork cloudNetwork)
			throws CloudProvisioningException {
		final ManagementNetwork mn = cloudNetwork.getManagement();
		if (mn != null) {
			this.validateNetworkName(mn.getNetworkConfiguration());
		}
		final Collection<NetworkConfiguration> templates =
				cloudNetwork.getTemplates().values();
		if (templates != null && !templates.isEmpty()) {
			for (NetworkConfiguration nc : templates) {
				this.validateNetworkName(nc);
			}
		}
	}

	private void validateNetworkName(final NetworkConfiguration networkConfiguration)
			throws CloudProvisioningException {
		if (networkConfiguration != null) {

			if (networkConfiguration.getName() == null && !networkConfiguration.getSubnets().isEmpty()) {
				throw new CloudProvisioningException("Network templates must have name.");
			}

			final List<org.cloudifysource.domain.cloud.network.Subnet> subnets = networkConfiguration.getSubnets();
			if (subnets != null && !subnets.isEmpty()) {
				for (final org.cloudifysource.domain.cloud.network.Subnet sn : subnets) {
					if (sn.getName() == null || sn.getName().trim().equals("")) {
						throw new CloudProvisioningException(
								"The name of the Subnet must be provided, Please check network block :'"
										+ networkConfiguration.getName() + "'");
					}
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if the configuration need the creation of an external router.
	 * 
	 * @return Returns <code>true</code> if the configuration need the creation of an external router.
	 */
	public boolean isCreateExternalRouter() {
		return this.getExternalRouterName() == null;
	}

	/**
	 * Returns the external router name from the configuration.
	 * 
	 * @return Returns the external router's name from the configuration.
	 */
	public String getExternalRouterName() {
		if (management) {
			final String extRouterName =
					(String) managementNetworkOptions.get(OpenStackCloudifyDriver.OPT_EXTERNAL_ROUTER_NAME);
			return StringUtils.isEmpty(extRouterName) ? null : extRouterName;
		}
		return null;
	}

	/***
	 * Returns <code>false</code> if the configuration need has specified an external network to use.
	 * 
	 * @return Returns <code>false</code> if the configuration need has specified an external network to use.
	 */
	public boolean isExternalNetworkNameSpecified() {
		return this.getExternalNetworkName() == null;
	}

	/**
	 * Returns the external network name from the configuration.
	 * 
	 * @return Returns the external network name from the configuration.
	 */
	public String getExternalNetworkName() {
		if (management) {
			final String extNetName =
					(String) managementNetworkOptions.get(OpenStackCloudifyDriver.OPT_EXTERNAL_NETWORK_NAME);
			return StringUtils.isEmpty(extNetName) ? null : extNetName;
		}
		return null;
	}

	/***
	 * Returns <code>false</code> if the configuration requires to create and to configure a router to an external
	 * network.
	 * 
	 * @return Returns <code>false</code> if the configuration need has specified an external network to use.
	 */
	public boolean skipExternalNetworking() {
		if (management) {
			final String skipExtNetStr =
					(String) managementNetworkOptions.get(OpenStackCloudifyDriver.OPT_SKIP_EXTERNAL_NETWORKING);
			return BooleanUtils.toBoolean(skipExtNetStr);
		}
		return false;
	}

	/***
	 * Returns <code>true</code> if requires floating IP.
	 * 
	 * @return Returns <code>true</code> if requires floating IP.
	 */
	public boolean associateFloatingIp() {
		if (management && this.managementNetworkConfiguration != null) {
			final String associate =
					this.managementNetworkConfiguration.getCustom().get(ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP);
			return BooleanUtils.toBoolean(associate);
		} else if (this.serviceNetworkConfiguration != null) {
			final String associate =
					this.serviceNetworkConfiguration.getCustom().get(ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP);
			return BooleanUtils.toBoolean(associate);
		}
		return false;
	}

}
