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
import java.util.logging.Logger;

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
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;

/**
 * This is a utility class to handle network configurations making life easier in the OpenStack driver.<br />
 * The implementation is dedicated to the driver and should be instantiate only by the setConfig of the driver.<br />
 * <br />
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
class OpenStackNetworkConfigurationHelper {

	private final Logger logger = Logger.getLogger(BaseProvisioningDriver.class.getName());

	private static final String ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP = "associateFloatingIpOnBootstrap";

	private boolean management;

	private NetworkConfiguration managementNetworkConfiguration;
	private NetworkConfiguration applicationNetworkConfiguration;

	private List<String> computeNetworks;
	private AccessRules serviceAccessRules;

	private Map<String, Object> managementNetworkOptions;

	private String managementNetworkPrefixName;
	private String applicationNetworkPrefixName;

	/**
	 * Constructor for testing purpose.
	 * 
	 * @param openstackPrefixNames
	 * @param configuration
	 */
	OpenStackNetworkConfigurationHelper() {
	}

	OpenStackNetworkConfigurationHelper(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {

		this.management = configuration.isManagement();

		final String name = configuration.isManagement() ? "managers" : configuration.getServiceName();
		logger.info("Setup network configuration for " + name);

		this.initManagementNetworkConfig(configuration);

		this.managementNetworkPrefixName = configuration.getCloud().getProvider().getManagementGroup();

		if (!this.management) {
			final FullServiceName fsn = ServiceUtils.getFullServiceName(configuration.getServiceName());
			this.applicationNetworkPrefixName = this.managementNetworkPrefixName + fsn.getApplicationName() + "-";
			this.initServiceNetworkConfig(configuration);
		}

	}

	/**
	 * Initialize properties for management network configuration.<br />
	 * A network must be provided for management either it is configured in <code>cloudNetwork</code> section or in
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
		final String templateName = cloud.getConfiguration().getManagementMachineTemplate();

		if (configuration.isManagement()) {
			// Init management computeNetworks. Check if there is any.
			final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
			if (computeTemplate != null) {
				final ComputeTemplateNetwork computeNetwork = computeTemplate.getComputeNetwork();
				if (computeNetwork != null) {
					this.computeNetworks = computeNetwork.getNetworks();
				}
				this.managementNetworkOptions = computeTemplate.getOptions();
			}

			if (computeNetworks == null) {
				this.computeNetworks = new ArrayList<String>();
			}
		}

		// Figure out if there is a management network.
		final NetworkConfiguration mngConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
		if (mngConfig != null && mngConfig.getName() != null && mngConfig.getSubnets() != null
				&& !mngConfig.getSubnets().isEmpty()) {
			this.managementNetworkConfiguration = mngConfig;
		}

		// Logs..
		if (this.management) {
			if (this.useManagementNetwork()) {
				logger.info("Using management network : " + this.managementNetworkConfiguration.getName());
			} else {
				logger.info("Using computeNetwork of template '" + templateName + "' : " + this.computeNetworks);
			}
		}
	}

	/**
	 * Initialize properties for service network configuration.<br />
	 * 
	 * @param configuration
	 *            The configuration.
	 * @throws CloudProvisioningException
	 *             Thrown if the service declared a network template which do not exist in the list of templates or if
	 *             no networks has been defined for cloudify communications.
	 */
	private void initServiceNetworkConfig(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {

		// Init the service computeNetworks. Check if there is any.
		final Map<String, ComputeTemplate> computeTemplates = configuration.getCloud().getCloudCompute().getTemplates();
		final ComputeTemplate computeTemplate = computeTemplates.get(configuration.getCloudTemplate());
		if (computeTemplate != null) {
			final ComputeTemplateNetwork computeNetwork = computeTemplate.getComputeNetwork();
			if (computeNetwork != null) {
				this.computeNetworks = computeNetwork.getNetworks();
			}
		}
		if (this.computeNetworks == null) {
			this.computeNetworks = new ArrayList<String>();
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
			this.applicationNetworkConfiguration = templates.get(serviceNetwork.getTemplate());
			if (this.applicationNetworkConfiguration == null) {
				final String message = "Service network template not found '" + serviceNetwork.getTemplate() + "'";
				logger.severe(message);
				throw new CloudProvisioningException(message);
			}
		}

		// Log private ip network
		logger.info("Service '" + configuration.getServiceName() + "' using network '" + this.getPrivateIpNetworkName()
				+ "' for private ip");
	}

	/**
	 * Returns the management network template configuration.
	 * 
	 * @return The management network template configuration.
	 */
	public NetworkConfiguration getManagementNetworkTemplate() {
		return this.managementNetworkConfiguration;
	}

	/**
	 * Returns the application network template configuration.
	 * 
	 * @return The application network template configuration.
	 */
	public NetworkConfiguration getApplicationNetworkTemplate() {
		return this.applicationNetworkConfiguration;
	}

	/**
	 * Returns <true> if the network configuration uses a management network, returns <code>false</code> otherwise.
	 * 
	 * @return Returns <true> if the network configuration uses a management network, returns <code>false</code>
	 *         otherwise.
	 */
	public boolean useManagementNetwork() {
		return this.getManagementNetworkPrefixedName() != null;
	}

	/**
	 * Returns <true> if the service recipe uses network template otherwise returns <code>false</code>.
	 * 
	 * @return Returns <true> if the service recipe uses network template, returns <code>false</code> otherwise.
	 */
	public boolean useApplicationNetworkTemplate() {
		return this.applicationNetworkConfiguration != null;
	}

	/**
	 * Returns the management network name prefixed with the management group name.<br />
	 * 
	 * @return Returns the management network prefixed name.
	 */
	public String getManagementNetworkPrefixedName() {
		if (this.managementNetworkConfiguration != null) {
			return managementNetworkPrefixName + managementNetworkConfiguration.getName();
		}
		return null;
	}

	/**
	 * Returns the application network name prefixed with the management group name and application name.<br />
	 * 
	 * @return Returns the application network prefixed name.
	 */
	public String getApplicationNetworkPrefixedName() {
		if (this.applicationNetworkConfiguration != null) {
			return applicationNetworkPrefixName + applicationNetworkConfiguration.getName();
		}
		return null;

	}

	/**
	 * <p>
	 * Returns the name of the private IP network to use.
	 * </p>
	 * <p>
	 * <ol>
	 * <li>The management network if exists.</li>
	 * <li>Otherwise, it's the first network defined in the computeNetwork (from the cloudCompute in groovy DSL).</li>
	 * </ol>
	 * </p>
	 * 
	 * @return Returns the name of the private IP network to use.
	 */
	public String getPrivateIpNetworkName() {
		String name = null;
		if (this.useManagementNetwork()) {
			// If there is a management network then there it is
			name = this.getManagementNetworkPrefixedName();
		} else {
			name = computeNetworks.get(0);
		}
		return name;
	}

	/**
	 * Returns the network names defined in the computeNetworks.
	 * 
	 * @return Returns the network names defined in the computeNetworks.
	 */
	public List<String> getComputeNetworks() {
		return this.computeNetworks;
	}

	public AccessRules getServiceAccessRules() {
		return serviceAccessRules;
	}

	private boolean isValidDefiniton(final String definition) {

		if (definition == null || StringUtils.trim(definition).isEmpty()) {
			return false;
		}
		return true;

	}

	public boolean isValidSubnetName(final org.cloudifysource.domain.cloud.network.Subnet subnet) {
		return isValidDefiniton(subnet.getName());
	}

	public boolean isValidSubnetRange(final org.cloudifysource.domain.cloud.network.Subnet subnet) {
		return isValidDefiniton(subnet.getRange());
	}

	public boolean isValidNetworkName(final NetworkConfiguration networkConfiguration) {

		return isValidDefiniton(networkConfiguration.getName());
	}

	/**
	 * Verify that all networks and subnets has a name (including management one).
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

	public void validateNetworkName(final NetworkConfiguration networkConfiguration)
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
		} else if (this.applicationNetworkConfiguration != null) {
			final String associate =
					this.applicationNetworkConfiguration.getCustom().get(ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP);
			return BooleanUtils.toBoolean(associate);
		} else if (!management && this.applicationNetworkConfiguration == null && this.computeNetworks.isEmpty()) {
			// We are using management networks only.
			final String associate = this.managementNetworkConfiguration.getCustom()
					.get(ASSOCIATE_FLOATING_IP_ON_BOOTSTRAP);
			return BooleanUtils.toBoolean(associate);
		}
		return false;
	}
}
