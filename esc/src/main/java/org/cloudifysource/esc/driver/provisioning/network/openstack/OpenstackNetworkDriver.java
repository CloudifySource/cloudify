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
package org.cloudifysource.esc.driver.provisioning.network.openstack;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.network.ManagementNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.network.BaseNetworkDriver;
import org.cloudifysource.esc.driver.provisioning.network.NetworkDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.network.NetworkProvisioningException;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackNetworkClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackJsonSerializationException;
import org.cloudifysource.esc.driver.provisioning.openstack.SecurityGroupNames;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;

/**
 * Network driver for Openstack.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenstackNetworkDriver extends BaseNetworkDriver {

	private static Logger logger = Logger.getLogger(OpenstackNetworkDriver.class.getName());

	private OpenStackNetworkClient networkApi;
	private SecurityGroupNames securityGroupNames;

	@Override
	public void setConfig(final NetworkDriverConfiguration config) {
		super.setConfig(config);
		final Cloud cloud = config.getCloud();
		this.initDeployer(cloud);
		String mngGroup = cloud.getProvider().getManagementGroup();
		mngGroup = mngGroup == null ? OpenStackCloudifyDriver.getDefaultMangementPrefix() : mngGroup;
		this.securityGroupNames = new SecurityGroupNames(mngGroup, null, null);
	}

	private void initDeployer(final Cloud cloud) {
		final String managementMachineTemplate = cloud.getConfiguration().getManagementMachineTemplate();
		final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);

		String endpoint = null;
		final Map<String, Object> overrides = cloudTemplate.getOverrides();
		if (overrides != null && !overrides.isEmpty()) {
			endpoint = (String) overrides.get(OpenStackCloudifyDriver.OPENSTACK_ENDPOINT);
		}

		final String networkVersion =
				(String) cloudTemplate.getOptions().get(OpenStackCloudifyDriver.OPT_NETWORK_API_VERSION);
		final String networkServiceName =
				(String) cloudTemplate.getOptions().get(OpenStackCloudifyDriver.OPT_NETWORK_SERVICE_NAME);

		final String cloudImageId = cloudTemplate.getImageId();
		final String region = cloudImageId.split("/")[0];

		final String cloudUser = cloud.getUser().getUser();
		final String password = cloud.getUser().getApiKey();

		final StringTokenizer st = new StringTokenizer(cloudUser, ":");
		final String tenant = st.hasMoreElements() ? (String) st.nextToken() : null;
		final String username = st.hasMoreElements() ? (String) st.nextToken() : null;

		try {
			this.networkApi = new OpenStackNetworkClient(endpoint, username, password, tenant, region,
					networkServiceName, networkVersion);
		} catch (OpenstackJsonSerializationException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context, final long duration,
			final TimeUnit timeUnit) throws NetworkProvisioningException, TimeoutException {
		// poolname = external network's name
		try {
			final FloatingIp floatingIp = networkApi.allocateFloatingIp(poolName);
			if (floatingIp == null) {
				throw new NetworkProvisioningException("Floating ip has not been created from the pool '" + poolName
						+ "'");
			}
			return floatingIp.getFloatingIpAddress();
		} catch (final OpenstackException e) {
			throw new NetworkProvisioningException(e);
		}

	}

	@Override
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIp,
			final Map<String, Object> context, final long duration, final TimeUnit timeUnit)
			throws NetworkProvisioningException, TimeoutException {
		try {
			final FloatingIp floatingIpByIp = networkApi.getFloatingIpByIp(floatingIp);
			final Network networkByName = this.getManagementNetwork();
			final Port port = networkApi.getPort(instanceIPAddress, networkByName.getId());
			if (floatingIpByIp == null || port == null) {
				throw new NetworkProvisioningException(
						"Couldn't assign floating ip. Missing floating ip in the pool or port does not exists.");
			}
			networkApi.assignFloatingIp(floatingIpByIp.getId(), port.getId());
		} catch (final OpenstackException e) {
			throw new NetworkProvisioningException(e);
		}

	}

	private Network getManagementNetwork() throws OpenstackException, NetworkProvisioningException {
		final ManagementNetwork managementNetwork = this.networkConfig.getCloud().getCloudNetwork().getManagement();
		final NetworkConfiguration managementNetworkConfig = managementNetwork.getNetworkConfiguration();
		final String managementNetworkName = securityGroupNames.getPrefix() + managementNetworkConfig.getName();
		final Network networkByName = networkApi.getNetworkByName(managementNetworkName);
		if (networkByName == null) {
			throw new NetworkProvisioningException("Couldn't find the cloudify management network.");
		}
		return networkByName;
	}

	@Override
	public void releaseFloatingIP(final String floatingIP, final Map<String, Object> context, final long duration,
			final TimeUnit timeUnit) throws NetworkProvisioningException, TimeoutException {
		try {
			final FloatingIp floatingIp = networkApi.getFloatingIpByIp(floatingIP);
			if (floatingIp == null) {
				logger.warning("Floating ip not found ip='" + floatingIP + "'. May already be released.");
			} else {
				networkApi.releaseFloatingIp(floatingIp.getId());
			}
		} catch (final OpenstackException e) {
			throw new NetworkProvisioningException(e);
		}
	}

	@Override
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit) throws NetworkProvisioningException, TimeoutException {
		try {
			final FloatingIp floatingIp = networkApi.getFloatingIpByIp(floatingIP);
			if (floatingIp == null) {
				throw new NetworkProvisioningException("Floating ip not found ip='" + floatingIP + "'");
			}
			final Network networkByName = this.getManagementNetwork();
			final Port port = networkApi.getPort(instanceIPAddress, networkByName.getId());
			if (port == null) {
				logger.warning("Could not unassigned floating ip. No floating ip '" + floatingIP
						+ "' assiciated to instance '" + instanceIPAddress + "'");
			}
			networkApi.unassignFloatingIp(floatingIp.getId());
		} catch (final OpenstackException e) {
			throw new NetworkProvisioningException(e);
		}
	}
}
