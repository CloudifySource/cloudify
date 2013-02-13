/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.byon;

import groovy.lang.Closure;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.util.IPUtils;

/**
 *
 * @author yael
 * @since 2.5.0
 *
 */
public class DynamicByonProvisioningDriver extends BaseProvisioningDriver {

	private static String PROVIDER_ID = "dynamic-byon";
	private final AtomicInteger idCounter = new AtomicInteger(0);
	private final Object mutex = new Object();
	private final LinkedList<String> managementMachines = new LinkedList<String>();

	@Override
	public MachineDetails startMachine(String locationId, long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		String currnentId = PROVIDER_ID + "{" + idCounter.getAndIncrement() + "}";
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(cloudTemplateName);

		Map<String, Object> custom = template.getCustom();
		Closure<String> getNodeClosure =  (Closure<String>) custom.get(CloudifyConstants.DYNAMIC_BYON_START_MACHINE_KEY);
		String ip = getNodeClosure.call();

		return createMachine(currnentId, template, ip);
	}

	@Override
	public MachineDetails[] startManagementMachines(long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);

		final ComputeTemplate managementTemplate =
				this.cloud.getCloudCompute().getTemplates().get(this.cloud.getConfiguration().getManagementMachineTemplate());
		Map<String, Object> custom = managementTemplate.getCustom();
		@SuppressWarnings("unchecked")
		Closure<List<String>> getNodesClosure =  (Closure<List<String>>) custom.get(CloudifyConstants.DYNAMIC_BYON_START_MNG_MACHINES_KEY);
		List<String> ips = getNodesClosure.call();

		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final int size = ips.size();
		if (size != numberOfManagementMachines) {
			throw new CloudProvisioningException("DynamicByonProvisioningDriver [startManagementMachines] - expected "
					+ numberOfManagementMachines + " management machines, but got " + size + " machines.");
		}
		synchronized (mutex) {
			managementMachines.addAll(ips);
		}

		logger.info("Starting " + numberOfManagementMachines + " management machines.");
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		final MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		logger.info("Successfully added " + numberOfManagementMachines + " management machines: " + ips);
		return createdMachines;
	}

	@Override
	public boolean stopMachine(String machineIp, long duration, TimeUnit unit)
			throws InterruptedException, TimeoutException,
			CloudProvisioningException {
		logger.info("Stopping machine [" + machineIp + "]");
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(cloudTemplateName);
		final Map<String, Object> custom = template.getCustom();
		Closure<?> stopClosure =  (Closure<?>) custom.get(CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY);
		stopClosure.call(machineIp);
		return true;
	}

	@Override
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {
		ComputeTemplate template = cloud.getCloudCompute().getTemplates()
				.get(cloud.getConfiguration().getManagementMachineTemplate());
		Map<String, Object> custom = template.getCustom();
		Closure<?> stopClosure =  (Closure<?>) custom.get(CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY);
		stopClosure.call();
		synchronized (mutex) {
			managementMachines.clear();
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initDeployer(Cloud cloud) {
		// TODO Auto-generated method stub
	}

	@Override
	protected MachineDetails createServer(String serverName, long endTime, ComputeTemplate template) 
			throws CloudProvisioningException, TimeoutException {

		String ip;
		synchronized (mutex) {
			ip = managementMachines.removeFirst();
		}

		MachineDetails machine = createMachine(serverName, template, ip);

		if (System.currentTimeMillis() > endTime) {
			throw new TimeoutException();
		}
		logger.info("Successfully started machine [" + ip + "]");

		return machine;
	}

	private MachineDetails createMachine(String serverName,
			ComputeTemplate template, String ip) throws CloudProvisioningException {
		final CustomNodeImpl customNode = new CustomNodeImpl(PROVIDER_ID, serverName, ip,
				template.getUsername(), template.getPassword(), serverName);

		try {
			String resolvedIP = IPUtils.resolveHostName(customNode.getPrivateIP());
			customNode.setResolvedIP(resolvedIP);
			if (template.getRemoteExecution() == RemoteExecutionModes.WINRM) {
				customNode.setLoginPort(RemoteExecutionModes.WINRM.getDefaultPort());
			}
			IPUtils.validateConnection(customNode.getResolvedIP(), customNode.getLoginPort());
		} catch (Exception e) {
			logger.log(
					Level.INFO,
					"Failed to create server on " + customNode.getPrivateIP()
					+ ", connection failed on port "
					+ customNode.getLoginPort(), e);
			throw new CloudProvisioningException(e);
		}
		return createMachineDetails(customNode, template);
	}

	@Override
	protected void handleProvisioningFailure(int numberOfManagementMachines, int numberOfErrors, Exception firstCreationException,
			MachineDetails[] createdManagementMachines) throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines + " management machines, " + numberOfErrors
				+ " failed to start.");
		publishEvent("prov_management_machines_failed", firstCreationException.getMessage());
		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	private MachineDetails createMachineDetails(CustomNodeImpl customNode, ComputeTemplate template) throws CloudProvisioningException {
		MachineDetails machineDetails = new MachineDetails();

		machineDetails.setAgentRunning(false);
		machineDetails.setCloudifyInstalled(false);
		machineDetails.setInstallationDirectory(null);
		machineDetails.setMachineId(customNode.getId());
		machineDetails.setPrivateAddress(customNode.getPrivateIP());
		machineDetails.setPublicAddress(customNode.getPublicIP());

		// if the node has user/pwd - use it. Otherwise - take the use/password from the template's settings.
		if (!StringUtils.isBlank(customNode.getUsername()) && !StringUtils.isBlank(customNode.getCredential())) {
			machineDetails.setRemoteUsername(customNode.getUsername());
			machineDetails.setRemotePassword(customNode.getCredential());
		} else if (!StringUtils.isBlank(template .getUsername())
				&& !StringUtils.isBlank(template.getPassword())) {
			machineDetails.setRemoteUsername(template.getUsername());
			machineDetails.setRemotePassword(template.getPassword());
		} else {
			String nodeStr = customNode.toString();
			logger.severe("Cloud node loading failed, missing credentials for server: " + nodeStr);
			publishEvent("prov_node_loading_failed", nodeStr);
			throw new CloudProvisioningException("Cloud node loading failed, missing credentials for server: "
					+ nodeStr);
		}

		machineDetails.setRemoteExecutionMode(template.getRemoteExecution());
		machineDetails.setFileTransferMode(template.getFileTransfer());

		return machineDetails;
	}

	@Override
	public Object getComputeContext() {
		return null;
	}

}
