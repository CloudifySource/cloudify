/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.LinuxProvisioningConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Listener;
import org.cloudifysource.esc.driver.provisioning.azure.model.Listeners;
import org.cloudifysource.esc.driver.provisioning.azure.model.OSVirtualHardDisk;
import org.cloudifysource.esc.driver.provisioning.azure.model.RestartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleList;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;
import org.cloudifysource.esc.driver.provisioning.azure.model.WinRm;
import org.cloudifysource.esc.driver.provisioning.azure.model.WindowsProvisioningConfigurationSet;

import com.sun.jersey.core.util.Base64;

import edu.emory.mathcs.backport.java.util.Arrays;

/*****************************************************************************************
 * this class is used for creating object instances representing the azure domain model. 
 * @author elip																			 
 * 																						 
 ******************************************************************************************/

public class MicrosoftAzureRequestBodyBuilder {

	private static final String UTF_8 = "UTF-8";
	
	private String affinityPrefix;
	private String cloudServicePrefix;
	private String storagePrefix;

	public MicrosoftAzureRequestBodyBuilder(final String affinityPrefix,
			final String cloudServicePrefix, final String storagePrefix) {
		this.affinityPrefix = affinityPrefix;
		this.cloudServicePrefix = cloudServicePrefix;
		this.storagePrefix = storagePrefix;
	}

	private static final int UUID_LENGTH = 8;

	/**
	 * 
	 * @param name
	 *            - the affinity group name.
	 * @param location
	 *            - the affinity group location.
	 * @return - an object representing a body of the create affinity request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/gg715317.aspx"
	 *         >Create Affinity Group</a>
	 */
	public CreateAffinityGroup buildCreateAffinity(final String name,
			final String location) {

		CreateAffinityGroup affinityGroup = new CreateAffinityGroup();
		affinityGroup.setName(name);
		String affinityGroupName = affinityPrefix
				+ generateRandomUUID(UUID_LENGTH);
		try {
			affinityGroup.setLabel(new String(Base64.encode(affinityGroupName), UTF_8));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		affinityGroup.setLocation(location);

		return affinityGroup;
	}

	/**
	 * 
	 * @param affinityGroup
	 *            - the affinity group to be associated with the cloud service
	 * @return - an object representing a body of the create cloud service
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/gg441304.aspx"
	 *         >Create Hosted Service</a>
	 */
	public CreateHostedService buildCreateCloudService(
			final String affinityGroup) {

		CreateHostedService hostedService = new CreateHostedService();
		hostedService.setAffinityGroup(affinityGroup);
		
		String randomUID = generateRandomUUID(UUID_LENGTH);
		
		try {
			hostedService.setLabel(new String(Base64
					.encode(cloudServicePrefix
							+ randomUID), UTF_8));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		hostedService.setServiceName(cloudServicePrefix
				+ randomUID);

		return hostedService;
	}

	/**
	 * 
	 * @param addressPrefix
	 *            - CIDR notation address space range
	 * @param affinityGroupName
	 *            - the affinity group associated with the network
	 * @param networkName
	 *            - the name of the network to create.
	 * @return - an object representing a body of the set network configuration
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/jj157181.aspx"
	 *         >Set Network Configuration</a>
	 */
	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(
			final String addressPrefix, final String affinityGroupName,
			final String networkName) {

		GlobalNetworkConfiguration networkConfiguration = new GlobalNetworkConfiguration();

		VirtualNetworkConfiguration virtualNetworkConfiguration = new VirtualNetworkConfiguration();

		VirtualNetworkSites virtualNetworkSites = new VirtualNetworkSites();

		VirtualNetworkSite virtualNetworkSite = new VirtualNetworkSite();

		AddressSpace addressSpace = new AddressSpace();
		addressSpace.setAddressPrefix(addressPrefix);

		virtualNetworkSite.setAddressSpace(addressSpace);
		virtualNetworkSite.setAffinityGroup(affinityGroupName);
		virtualNetworkSite.setName(networkName);

		virtualNetworkSites.getVirtualNetworkSites().add(virtualNetworkSite);
		virtualNetworkConfiguration.setVirtualNetworkSites(virtualNetworkSites);
		networkConfiguration
				.setVirtualNetworkConfiguration(virtualNetworkConfiguration);

		return networkConfiguration;
	}
	
	/**
	 * 
	 * @param sites - virtual network sites to deploy.
	 * @return - an object representing a body of the set network configuration
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/jj157181.aspx"
	 *         >Set Network Configuration</a>
	 */
	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(
			final List<VirtualNetworkSite> sites) {

		GlobalNetworkConfiguration networkConfiguration = new GlobalNetworkConfiguration();

		VirtualNetworkConfiguration virtualNetworkConfiguration = new VirtualNetworkConfiguration();

		VirtualNetworkSites virtualNetworkSites = new VirtualNetworkSites();

		virtualNetworkSites.setVirtualNetworkSites(sites);
		virtualNetworkConfiguration.setVirtualNetworkSites(virtualNetworkSites);
		networkConfiguration
				.setVirtualNetworkConfiguration(virtualNetworkConfiguration);

		return networkConfiguration;
	}
	
	/**
	 * 
	 * @param desc .
	 * @return  - an object representing a body of the create virtual machine deployment
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/jj157194.aspx"
	 *         >Create Virtual Machine Deployment</a>
	 */
	public Deployment buildDeployment(final CreatePersistentVMRoleDeploymentDescriptor desc, 
									final boolean isWindows) {

		String deploymentSlot = desc.getDeploymentSlot();
		String imageName = desc.getImageName();
		String storageAccountName = desc.getStorageAccountName();
		String userName = desc.getUserName();
		String password = desc.getPassword();
		String networkName = desc.getNetworkName();
		String size = desc.getSize();
		String deploymentName = desc.getDeploymentName();
		InputEndpoints endPoints = desc.getInputEndpoints();
		String roleName = desc.getRoleName();

		Deployment deployment = new Deployment();
		deployment.setDeploymentSlot(deploymentSlot);
		deployment.setDeploymentName(roleName);
		deployment.setVirtualNetworkName(networkName);
		deployment.setLabel(deploymentName);
		deployment.setName(deploymentName);

		RoleList roleList = new RoleList();

		Role role = new Role();
		role.setRoleType("PersistentVMRole");
		role.setRoleName(roleName);
		role.setRoleSize(size);

		OSVirtualHardDisk osVirtualHardDisk = new OSVirtualHardDisk();
		osVirtualHardDisk.setSourceImageName(imageName);

		String mediaLink = "https://" + storageAccountName
				+ ".blob.core.windows.net/vhds/" + deploymentName + ".vhd";
		osVirtualHardDisk.setMediaLink(mediaLink);

		role.setOSVirtualHardDisk(osVirtualHardDisk);

		ConfigurationSets configurationSets = new ConfigurationSets();

		if ( isWindows ) {
			
			// Windows Specific : roleName de la forme cloudify_manager_roled57f => ROLED57F (15 car limit size limit)
			String[] computerNameArray = roleName.split("_");
			String computerName = (computerNameArray[2]).toUpperCase();
			
			WindowsProvisioningConfigurationSet windowsProvisioningSet = new WindowsProvisioningConfigurationSet();	
			windowsProvisioningSet.setDisableSshPasswordAuthentication(true);
			windowsProvisioningSet.setHostName(roleName);
			windowsProvisioningSet.setUserName(userName);
			windowsProvisioningSet.setUserPassword(password);
			windowsProvisioningSet.setAdminPassword(password);
			windowsProvisioningSet.setComputerName(computerName); // (not optional) Windows ComputerName
			configurationSets.getConfigurationSets().add(windowsProvisioningSet);
			
			// Set WinRM : HTTP without Certificate
			WinRm winRm = new WinRm();
			Listeners listeners = new Listeners();
			Listener listener = new Listener();
			listener.setCertificateThumbprint(null); // Configure for Secure Winrm command (?)
			listener.setType("HTTP");
			listeners.getListeners().add(listener);
			winRm.setListeners(listeners);
		}
		else {
			LinuxProvisioningConfigurationSet linuxProvisioningSet = new LinuxProvisioningConfigurationSet();
			linuxProvisioningSet.setDisableSshPasswordAuthentication(true);
			linuxProvisioningSet.setHostName(roleName);
			linuxProvisioningSet.setUserName(userName);
			linuxProvisioningSet.setUserPassword(password);
			configurationSets.getConfigurationSets().add(linuxProvisioningSet);
		}


		// NetworkConfiguration
		NetworkConfigurationSet networkConfiguration = new NetworkConfigurationSet();

		networkConfiguration.setInputEndpoints(endPoints);

		configurationSets.getConfigurationSets().add(networkConfiguration);

		role.setConfigurationSets(configurationSets);

		roleList.getRoles().add(role);

		deployment.setRoleList(roleList);

		return deployment;
	}

	/**
	 * 
	 * @param affinityGroupName -
	 * 								the affinity group associated with this storage account.
	 * @param storageAccountName - 
	 * 								the storage account name.
	 *            
	 * @return - an object representing a body of the create storage service input
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/hh264518.aspx"
	 *         >Create Storage Service</a>
	 */
	public CreateStorageServiceInput buildCreateStorageAccount(
			final String affinityGroupName, final String storageAccountName) {

		CreateStorageServiceInput storageAccount = new CreateStorageServiceInput();
		storageAccount.setAffinityGroup(affinityGroupName);
		try {
			storageAccount.setLabel(new String(Base64
					.encode(storagePrefix), UTF_8));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		storageAccount.setServiceName(storageAccountName);

		return storageAccount;
	}
	
	/**
	 * 
	 * @return  - an object representing a body of the restart role
	 *         request. <br>
	 *         see <a href=
	 *         "http://msdn.microsoft.com/en-us/library/windowsazure/jj157197"
	 *         >Restart Role</a>
	 */
	public RestartRoleOperation buildRestartRoleOperation() {
		return new RestartRoleOperation();
	}

	private static String generateRandomUUID(final int length) {
		return UUIDHelper.generateRandomUUID(length);
	}

}
