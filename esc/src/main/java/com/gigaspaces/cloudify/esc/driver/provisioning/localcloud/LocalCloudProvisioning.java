package com.gigaspaces.cloudify.esc.driver.provisioning.localcloud;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.Admin;

import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.esc.driver.provisioning.CloudProvisioningException;
import com.gigaspaces.cloudify.esc.driver.provisioning.CloudifyProvisioning;
import com.gigaspaces.cloudify.esc.driver.provisioning.MachineDetails;

public class LocalCloudProvisioning implements CloudifyProvisioning {

	private boolean verbose;
	private String lookupGroups;
	private String nicAddress;
	private int teardownTimeoutInMinutes = 5;
	private int bootstrapTimeoutInMinutes = 5;


	
	@Override
	public void setConfig(Cloud2 cloud, String cloudTemplate, boolean management) {
		this.lookupGroups = cloud.getConfiguration().getLookupGroups();
		this.nicAddress= cloud.getConfiguration().getNicAddress();
		
		this.verbose = (Boolean) getConfigValue(cloud.getCustom(), "verbose",false);
		this.teardownTimeoutInMinutes = (Integer) getConfigValue(cloud.getCustom(), "teardownTimeoutInMinutes",5);
		this.bootstrapTimeoutInMinutes = (Integer) getConfigValue(cloud.getCustom(), "bootstrapTimeoutInMinutes",5);
	}
	
	private Object getConfigValue(Map<String, Object> config, String key, Object defaultValue) {
		final Object temp = config.get(key);
		if(temp == null) {
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
	public MachineDetails startMachine(long duration, TimeUnit unit)
			throws TimeoutException {
		throw new UnsupportedOperationException("Not available on local cloud");
	}

	@Override
	public boolean stopMachine(final String machineId) throws CloudProvisioningException{
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


	
}
