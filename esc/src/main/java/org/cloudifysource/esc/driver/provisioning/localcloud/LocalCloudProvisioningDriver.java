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
package org.cloudifysource.esc.driver.provisioning.localcloud;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
import org.openspaces.admin.Admin;

@Deprecated
public class LocalCloudProvisioningDriver implements ProvisioningDriver {

	private boolean verbose;
	private String lookupGroups;
	private String nicAddress;
	private int teardownTimeoutInMinutes = 5;
	private int bootstrapTimeoutInMinutes = 5;



	@Override
	public void setConfig(final Cloud cloud, final String cloudTemplate, final boolean management, 
			final String serviceName, final boolean performValidations) {
		this.lookupGroups = cloud.getConfiguration().getLookupGroups();
		this.nicAddress = cloud.getConfiguration().getNicAddress();

		this.verbose = (Boolean) getConfigValue(cloud.getCustom(), "verbose", Boolean.FALSE);
		this.teardownTimeoutInMinutes = (Integer) getConfigValue(cloud.getCustom(), "teardownTimeoutInMinutes",5);
		this.bootstrapTimeoutInMinutes = (Integer) getConfigValue(cloud.getCustom(), "bootstrapTimeoutInMinutes",5);
	}

	private Object getConfigValue(Map<String, Object> config, String key, Object defaultValue) {
		final Object temp = config.get(key);
		if (temp == null) {
			return defaultValue;
		} else {
			return temp;
		}
	}


//	public void bootstrapCloud(final Cloud2 cloud) throws CloudProvisioningException {
//
//		LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
//		installer.setVerbose(verbose);
//		installer.setLookupGroups(lookupGroups);
//		installer.setNicAddress(nicAddress);
//		installer.setProgressInSeconds(10);
//
//		try {
//			installer.startLocalCloudOnLocalhostAndWait(bootstrapTimeoutInMinutes , TimeUnit.MINUTES);
//		} catch (InterruptedException e) {
//			throw new CloudProvisioningException(
//					"Failed to start cloud machine: " + e.getMessage(), e);
//		} catch (TimeoutException e) {
//			throw new CloudProvisioningException(
//					"Failed to start cloud machine: " + e.getMessage(), e);
//		}
//
//	}

	@Override
	public MachineDetails startMachine(final String locationId, long duration, TimeUnit unit)
			throws TimeoutException {
		throw new UnsupportedOperationException("Not available on local cloud");
	}

	@Override
	public boolean stopMachine(final String machineId, final long duration, final TimeUnit unit) throws CloudProvisioningException{
		throw new UnsupportedOperationException("Not available on local cloud");

	}

	@Override
	public String getCloudName() {
		return "local-cloud";
	}

//	@Override
//	public void teardownCloud(final Cloud2 cloud) throws CloudProvisioningException {
//		LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
//		installer.setVerbose(verbose);
//		installer.setLookupGroups(lookupGroups);
//		installer.setNicAddress(nicAddress);
//		installer.setProgressInSeconds(10);
//
//		try {
//			installer.teardownLocalCloudOnLocalhostAndWait(
//					teardownTimeoutInMinutes, TimeUnit.MINUTES);
//		} catch (InterruptedException e) {
//			throw new CloudProvisioningException(
//					"Failed to tear down local cloud: " + e.getMessage(), e);
//		} catch (TimeoutException e) {
//			throw new CloudProvisioningException(
//					"Failed to tear down local cloud: " + e.getMessage(), e);
//		}
//
//	}


	///////////////////////////////////////
	// Getters used for testing purposes //
	///////////////////////////////////////
	public boolean isVerbose() {
		return verbose;
	}

	public String getLookupGroups() {
		return lookupGroups;
	}

	public String getNicAddress() {
		return nicAddress;
	}

	public int getTeardownTimeoutInMinutes() {
		return teardownTimeoutInMinutes;
	}

	public int getBootstrapTimeoutInMinutes() {
		return bootstrapTimeoutInMinutes;
	}

	@Override
	public void setAdmin(Admin admin) {
		// ignore - not required;
	}

	@Override
	public MachineDetails[] startManagementMachines(long duration, TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {
		// TODO - bootstrap local cloud on this machine
		throw new UnsupportedOperationException("Not available on local cloud");
	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {
		throw new UnsupportedOperationException("Not available on local cloud");

	}

	@Override
	public void close() {

	}

	@Override
	public void addListener(ProvisioningDriverListener pdl) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getComputeContext() {
		return null;
	}
}
