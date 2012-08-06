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

import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.LinuxProvisioningConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.OSVirtualHardDisk;
import org.cloudifysource.esc.driver.provisioning.azure.model.RestartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleList;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;

import com.sun.jersey.core.util.Base64;

/**
 * 
 * @author elip
 *
 */
public class MicrosoftAzureRequestBodyBuilder {

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_CLOUD_SERVICE_PREFIX = "cloudifycloudservice";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";
	private static final String CLOUDIFY_DEPLOYMENT_PREFIX = "cloudifydeployment";

	private static final int UUID_LENGTH = 8;
	private static final int UUID_ROLE_LENGTH = 4;
	
	/**
	 * 
	 * @param name - the affinity group name.
	 * @param location - the affinity group location.
	 * @return - an object representing a body of the create affinity request.
	 * <br>
	 * see <a href="http://msdn.microsoft.com/en-us/library/windowsazure/gg715317.aspx">Create Affinity Group</a>
	 */
	public CreateAffinityGroup buildCreateAffinity(final String name, final String location) {

		CreateAffinityGroup affinityGroup = new CreateAffinityGroup();
		affinityGroup.setName(name);
		affinityGroup.setLabel(new String(Base64
				.encode(CLOUDIFY_AFFINITY_PREFIX
						+ generateRandomUUID(UUID_LENGTH))));
		affinityGroup.setLocation(location);

		return affinityGroup;
	}
	
	/**
	 * 
	 * @param affinityGroup - the affinity group to be associated with the cloud service
	 * @return - an object representing a body of the create cloud service request.
	 * <br>
	 * see <a href="http://msdn.microsoft.com/en-us/library/windowsazure/gg441304.aspx">Create Hosted Service</a>
	 */
	public CreateHostedService buildCreateCloudService(final String affinityGroup) {

		CreateHostedService hostedService = new CreateHostedService();
		hostedService.setAffinityGroup(affinityGroup);
		hostedService.setLabel(new String(Base64
				.encode(CLOUDIFY_CLOUD_SERVICE_PREFIX
						+ generateRandomUUID(UUID_LENGTH))));
		hostedService.setServiceName(CLOUDIFY_CLOUD_SERVICE_PREFIX
				+ generateRandomUUID(UUID_LENGTH));

		return hostedService;
	}
	
	/**
	 * 
	 * @param addressPrefix - CIDR notation address space range
	 * @param affinityGroupName - the affinity group associated with the network
	 * @param networkName - the name of the network to create.
	 * @return - an object representing a body of the set network configuration request.
	 * <br>
	 * see <a href="http://msdn.microsoft.com/en-us/library/windowsazure/jj157181.aspx">Set Network Configuration</a>
	 */
	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(final String addressPrefix,
			final String affinityGroupName, final String networkName) {

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
	
	public Deployment buildDeployment(CreatePersistentVMRoleDeploymentDescriptor desc) {

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
		deployment.setVirtualNetworkName(networkName);
		deployment.setLabel(CLOUDIFY_DEPLOYMENT_PREFIX);
		deployment.setName(deploymentName);

		RoleList roleList = new RoleList();

		Role role = new Role();
		role.setRoleType("PersistentVMRole");
		role.setRoleName(roleName);
		role.setRoleSize(size);

		OSVirtualHardDisk osVirtualHardDisk = new OSVirtualHardDisk();
		osVirtualHardDisk.setSourceImageName(imageName);

		String mediaLink = "https://" + storageAccountName
				+ ".blob.core.windows.net/vhds/" + deploymentName 
				+ ".vhd";
		osVirtualHardDisk.setMediaLink(mediaLink);

		role.setOSVirtualHardDisk(osVirtualHardDisk);

		ConfigurationSets configurationSets = new ConfigurationSets();

		LinuxProvisioningConfigurationSet linuxProvisioningConfiguration = new LinuxProvisioningConfigurationSet();
		linuxProvisioningConfiguration
				.setDisableSshPasswordAuthentication(false);
		linuxProvisioningConfiguration.setHostName(roleName);
		linuxProvisioningConfiguration.setUserName(userName);
		linuxProvisioningConfiguration.setUserPassword(password);

		NetworkConfigurationSet networkConfiguration = new NetworkConfigurationSet();

		networkConfiguration.setInputEndpoints(endPoints);
		
		configurationSets.getConfigurationSets().add(linuxProvisioningConfiguration);
		configurationSets.getConfigurationSets().add(networkConfiguration);

		role.setConfigurationSets(configurationSets);

		roleList.getRoles().add(role);

		deployment.setRoleList(roleList);

		return deployment;
	}

	/**
	 * 
	 * @param affinityGroupName - the affinity group associated with this storage account.
	 * @return - an object representing a body of the create virtual machine deployment request.
	 */
	public CreateStorageServiceInput buildCreateStorageAccount(
			final String affinityGroupName, final String storageAccountName) {

		CreateStorageServiceInput storageAccount = new CreateStorageServiceInput();
		storageAccount.setAffinityGroup(affinityGroupName);
		storageAccount.setLabel(new String(Base64
				.encode(CLOUDIFY_STORAGE_ACCOUNT_PREFIX)));
		storageAccount.setServiceName(storageAccountName);

		return storageAccount;
	}
	
	public RestartRoleOperation buildRestartRoleOperation() {
		return new RestartRoleOperation();
	}

	private static String generateRandomUUID(final int length) {
		return UUIDHelper.generateRandomUUID(length);
	}

}
