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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.AttachedTo;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployments;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.Error;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Operation;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/********************************************************************************
 * A REST client implementation for the Azure REST API. this client is designed
 * for using azure infrastructure as an IaaS. each VM is provisioned onto a
 * separate cloud service that belong to the same virtual network site. this way
 * all VM's are assigned public and private IP. and all VM's can be either a
 * back end of a front end of you application. authentication is achieved by
 * using self-signed certificates (OpenSSL, makecert)
 * 
 * @author elip
 ********************************************************************************/

public class MicrosoftAzureRestClient {

	private static final int HTTP_NOT_FOUND = 404;
	private static final int HTTP_OK = 200;
	private static final int HTTP_CREATED = 201;
	private static final int HTTP_ACCEPTED = 202;

	private static final char BAD_CHAR = 65279;

	private String affinityPrefix;
	private String cloudServicePrefix;
	private String storagePrefix;

	private Lock pendingRequest = new ReentrantLock(true);

	private MicrosoftAzureRequestBodyBuilder requestBodyBuilder;

	// Azure Management Service API End Point
	private static final String CORE_MANAGEMENT_END_POINT = "https://management.core.windows.net/";

	// Header names and values
	private static final String X_MS_VERSION_HEADER_NAME = "x-ms-version";
	private static final String X_MS_VERSION_HEADER_VALUE = "2013-03-01";
	private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
	private static final String CONTENT_TYPE_HEADER_VALUE = "application/xml";

	private static final String FAILED = "Failed";
	private static final String SUCCEEDED = "Succeeded";
	private static final String IN_PROGRESS = "InProgress";

	private static final int MAX_RETRIES = 5;

	private static final long DEFAULT_POLLING_INTERVAL = 5 * 1000; // 5 seconds

	private static final long ESTIMATED_TIME_TO_START_VM = 5 * 60 * 1000; // 5
																			// minutes

	private WebResource resource;
	private Client client;

	private String subscriptionId;

	private MicrosoftAzureSSLHelper sslHelper;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public MicrosoftAzureRestClient(final String subscriptionId,
			final String pathToPfx, final String pfxPassword,
			final String affinityPrefix, final String cloudServicePrefix,
			final String storagePrefix) {
		this.subscriptionId = subscriptionId;
		this.affinityPrefix = affinityPrefix;
		this.cloudServicePrefix = cloudServicePrefix;
		this.storagePrefix = storagePrefix;
		this.init(pathToPfx, pfxPassword, affinityPrefix, cloudServicePrefix,
				storagePrefix);
	}

	public MicrosoftAzureRestClient() {

	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(final String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getAffinityPrefix() {
		return affinityPrefix;
	}

	public void setAffinityPrefix(final String affinityPrefix) {
		this.affinityPrefix = affinityPrefix;
	}

	public String getCloudServicePrefix() {
		return cloudServicePrefix;
	}

	public void setCloudServicePrefix(final String cloudServicePrefix) {
		this.cloudServicePrefix = cloudServicePrefix;
	}

	public String getStoragePrefix() {
		return storagePrefix;
	}

	public void setStoragePrefix(final String storagePrefix) {
		this.storagePrefix = storagePrefix;
	}

	/**
	 * 
	 * @param logger
	 *            - the logger to add to the client
	 */
	public void setLoggingFilter(final Logger logger) {
		this.client.addFilter(new LoggingFilter(logger));
	}

	private void init(final String pathToPfx, final String pfxPassword,
			final String affinityPrefix, final String cloudServicePrefix,
			final String storagePrefix) {
		try {
			this.sslHelper = new MicrosoftAzureSSLHelper(pathToPfx, pfxPassword);
			this.client = createClient(sslHelper.createSSLContext());
			this.resource = client.resource(CORE_MANAGEMENT_END_POINT);
			this.requestBodyBuilder = new MicrosoftAzureRequestBodyBuilder(
					affinityPrefix, cloudServicePrefix, storagePrefix);
		} catch (final Exception e) {
			throw new RuntimeException("Failed initializing rest client : " + e.getMessage() , e);
		}
	}

	private Client createClient(final SSLContext context) {
		ClientConfig config = new DefaultClientConfig();
		config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
				new HTTPSProperties(null, context));
		Client httpClient = Client.create(config);
		httpClient.setConnectTimeout(CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		httpClient.setReadTimeout(CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		return httpClient;
	}

	/**
	 * 
	 * @param affinityGroup
	 *            - the affinity group for the cloud service.
	 * @param endTime
	 *            .
	 * 
	 * @return - the newly created cloud service name.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public String createCloudService(final String affinityGroup,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		logger.fine(getThreadIdentity() + "Creating cloud service");

		CreateHostedService createHostedService = requestBodyBuilder
				.buildCreateCloudService(affinityGroup);

		String serviceName = null;
		try {
			String xmlRequest = MicrosoftAzureModelUtils.marshall(
					createHostedService, false);

			ClientResponse response = doPost("/services/hostedservices",
					xmlRequest);
			String requestId = extractRequestId(response);
			waitForRequestToFinish(requestId, endTime);
			serviceName = createHostedService.getServiceName();
			logger.info("Cloud service created : " + serviceName);
		} catch (final Exception e) {
			logger.warning("Failed to create cloud service : " + e.getMessage());
			if (e instanceof MicrosoftAzureException) {
				throw (MicrosoftAzureException)e;
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException)e;
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException)e;
			}
		}
		return serviceName;
	}

	/**
	 * this method creates a storage account with the given name, or does
	 * nothing if the account exists.
	 * 
	 * @param affinityGroup
	 *            - the affinity group for the storage account.
	 * @param storageAccountName
	 *            - the name for the storage account to create.
	 * @param endTime
	 *            .
	 * 
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createStorageAccount(final String affinityGroup,
			final String storageAccountName, final long endTime)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {

		CreateStorageServiceInput createStorageServiceInput = requestBodyBuilder
				.buildCreateStorageAccount(affinityGroup, storageAccountName);

		if (storageExists(storageAccountName)) {
			logger.info("Using an already existing storage account : "
					+ storageAccountName);
			return;
		}

		logger.info("Creating a storage account : " + storageAccountName);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createStorageServiceInput, false);
		ClientResponse response = doPost("/services/storageservices",
				xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);

		logger.fine("Created a storage account : " + storageAccountName);

	}

	/**
	 * this method creates a virtual network with the given name, or does
	 * nothing if the network exists.
	 * 
	 * @param addressSpace
	 *            - CIDR notation specifying the address space for the virtual
	 *            network.
	 * @param affinityGroup
	 *            - the affinity group for this virtual network
	 * @param networkSiteName
	 *            - the name for the network to create
	 * @param endTime
	 *            .
	 * 
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createVirtualNetworkSite(final String addressSpace,
			final String affinityGroup, final String networkSiteName,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		VirtualNetworkSites virtualNetworkSites = listVirtualNetworkSites();
		if (virtualNetworkSites != null
				&& virtualNetworkSites.contains(networkSiteName)) {
			logger.info("Using an already existing virtual netowrk site : "
					+ networkSiteName);
			return;
		} else {
			if (virtualNetworkSites == null) {
				virtualNetworkSites = new VirtualNetworkSites();
			}
		}

		logger.info("Creating virtual network site : " + networkSiteName);

		VirtualNetworkSite newSite = new VirtualNetworkSite();
		AddressSpace address = new AddressSpace();
		address.setAddressPrefix(addressSpace);
		newSite.setAddressSpace(address);
		newSite.setAffinityGroup(affinityGroup);
		newSite.setName(networkSiteName);

		virtualNetworkSites.getVirtualNetworkSites().add(newSite);

		setNetworkConfiguration(endTime, virtualNetworkSites);
		logger.fine("Created virtual network site : " + networkSiteName);
	}

	/**
	 * this method creates an affinity group with the given name, or does
	 * nothing if the group exists.
	 * 
	 * @param affinityGroup
	 *            - the name of the affinity group to create
	 * @param location
	 *            - one of MS Data Centers locations.
	 * @param endTime
	 *            .
	 * 
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createAffinityGroup(final String affinityGroup,
			final String location, final long endTime)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {

		CreateAffinityGroup createAffinityGroup = requestBodyBuilder
				.buildCreateAffinity(affinityGroup, location);

		if (affinityExists(affinityGroup)) {
			logger.info("Using an already existing affinity group : " + affinityGroup);
			return;
		}

		logger.info("Creating affinity group : " + affinityGroup);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createAffinityGroup, false);
		ClientResponse response = doPost("/affinitygroups", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Created affinity group : " + affinityGroup);
	}

	/**
	 * This method creates a virtual machine and a corresponding cloud service.
	 * the cloud service will use the affinity group specified by deploymentDesc.getAffinityGroup();
	 * If another request was made this method will wait until the pending request is finished.
	 * 
	 * If a failure happened after the cloud service was created, this method will delete it and throw.
	 * 
	 * @param deplyomentDesc
	 *            .
	 * @param endTime
	 *            .
	 * @return an instance of {@link RoleDetails} containing the ip addresses
	 *         information for the created role.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public RoleDetails createVirtualMachineDeployment(
			final CreatePersistentVMRoleDeploymentDescriptor deplyomentDesc,
			final boolean isWindows,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis
				- ESTIMATED_TIME_TO_START_VM;
		if (lockTimeout < 0) {
			throw new MicrosoftAzureException(
					"Aborted request to provision virtual machine. "
							+ "The timeout is less then the estimated time to provision the machine");
		}

		logger.fine(getThreadIdentity() + "Waiting for pending request lock for lock " + pendingRequest.hashCode());
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout,
				TimeUnit.MILLISECONDS);

		String serviceName = null;
		Deployment deployment;

		if (lockAcquired) {

			logger.fine(getThreadIdentity() + "Lock acquired : " + pendingRequest.hashCode());
			logger.fine(getThreadIdentity() + "Executing a request to provision a new virtual machine");

			try {

				serviceName = createCloudService(
						deplyomentDesc.getAffinityGroup(), endTime);

				deplyomentDesc.setHostedServiceName(serviceName);
				deplyomentDesc.setDeploymentName(serviceName);

				deployment = requestBodyBuilder.buildDeployment(deplyomentDesc,isWindows);

				String xmlRequest = MicrosoftAzureModelUtils.marshall(
						deployment, false);

				logger.fine(getThreadIdentity() + "Launching virtual machine : "
						+ deplyomentDesc.getRoleName());

				ClientResponse response = doPost("/services/hostedservices/"
						+ serviceName + "/deployments", xmlRequest);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				logger.fine(getThreadIdentity() + "About to release lock " + pendingRequest.hashCode());
				pendingRequest.unlock();
			} catch (final Exception e) {
				logger.fine(getThreadIdentity() + "A failure occured : about to release lock " 
							+ pendingRequest.hashCode());
				if (serviceName != null) {
					try {
						// delete the dedicated cloud service that was created for the virtual machine.
						deleteCloudService(serviceName, endTime);
					} catch (final Exception e1) {
						logger.warning("Failed deleting cloud service " + serviceName + " : " + e1.getMessage());
						logger.finest(ExceptionUtils.getFullStackTrace(e1));
					}
				}
				pendingRequest.unlock();
				if (e instanceof MicrosoftAzureException) {
					throw (MicrosoftAzureException)e;
				}
				if (e instanceof TimeoutException) {
					throw (TimeoutException)e;
				}
				if (e instanceof InterruptedException) {
					throw (InterruptedException)e;
				}
				throw new MicrosoftAzureException(e);
			}
		} else {
			throw new TimeoutException(
					"Failed to acquire lock for deleteDeployment request after + "
							+ lockTimeout + " milliseconds");
		}

		Deployment deploymentResponse = null;
		try {
			deploymentResponse = waitForDeploymentStatus("Running",
					serviceName, deployment.getDeploymentSlot(), endTime);

			deploymentResponse = waitForRoleInstanceStatus("ReadyRole",
					serviceName, deployment.getDeploymentSlot(), endTime);
		} catch (final Exception e) {
			logger.fine("Error while waiting for VM status : " +  e.getMessage());
			// the VM was created but with a bad status
			deleteVirtualMachineByDeploymentName(serviceName, deployment.getName(), endTime);
			if (e instanceof MicrosoftAzureException) {
				throw (MicrosoftAzureException) e;
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
			throw new MicrosoftAzureException(e);
		}

		RoleDetails roleAddressDetails = new RoleDetails();
		roleAddressDetails.setId(deploymentResponse.getPrivateId());
		roleAddressDetails
				.setPrivateIp(deploymentResponse.getRoleInstanceList()
						.getRoleInstances().get(0).getIpAddress());
		ConfigurationSets configurationSets = deploymentResponse.getRoleList()
				.getRoles().get(0).getConfigurationSets();

		String publicIp = null;
		for (ConfigurationSet configurationSet : configurationSets) {
			if (configurationSet instanceof NetworkConfigurationSet) {
				NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
				publicIp = networkConfigurationSet.getInputEndpoints()
						.getInputEndpoints().get(0).getvIp();
			}
		}
		roleAddressDetails.setPublicIp(publicIp);

		return roleAddressDetails;
	}

	/**
	 * @param vmStatus
	 */
	private boolean checkVirtualMachineStatusForError(final String vmStatus) {
		return (vmStatus.equals("FailedStartingRole")
				|| vmStatus.equals("FailedStartingVM")
				|| vmStatus.equals("UnresponsiveRole")
				|| vmStatus.equals("CyclingRole"));
	}

	/**
	 * 
	 * @return - the response body listing every available OS Image that belongs
	 *         to the subscription
	 * @throws MicrosoftAzureException
	 *             - indicates an exception was caught during the API call
	 * @throws TimeoutException .
	 */
	public String listOsImages() throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doGet("/services/images");
		checkForError(response);
		return response.getEntity(String.class);

	}

	/**
	 * This method deletes the storage account with the specified name. or does
	 * nothing if the storage account does not exist.
	 * @param storageAccountName
	 *            .
	 * @param endTime
	 *            .
	 * 
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteStorageAccount(final String storageAccountName,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {
		
		if (!storageExists(storageAccountName)) {
			return true;
		}
		
		logger.info("Deleting storage account : " + storageAccountName);
		ClientResponse response = doDelete("/services/storageservices/"
				+ storageAccountName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Deleted storage account : " + storageAccountName);
		return true;

	}

	/**
	 * This method deletes the affinity group with the specified name. or does
	 * nothing if the affinity group does not exist.
	 * @param affinityGroupName
	 *            .
	 * @param endTime
	 *            .
	 * @return true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteAffinityGroup(final String affinityGroupName,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {
		
		if (!affinityExists(affinityGroupName)) {
			return true;
		}
		
		logger.info("Deleting affinity group : " + affinityGroupName);
		ClientResponse response = doDelete("/affinitygroups/"
				+ affinityGroupName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Deleted affinity group : " + affinityGroupName);
		return true;
	}

	/**
	 * This method deletes the cloud service with the specified name. or does
	 * nothing if the cloud service does not exist.
	 * @param cloudServiceName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteCloudService(final String cloudServiceName,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {
		
		if (!cloudServiceExists(cloudServiceName)) {
			logger.info("Cloud service " + cloudServiceName + " does not exist.");
			return true;
		}
		
		logger.fine("Deleting cloud service : " + cloudServiceName);
		ClientResponse response = doDelete("/services/hostedservices/"
				+ cloudServiceName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;
	}

	/**
	 * 
	 * @param machineIp
	 *            - the machine ip.
	 * @param isPrivateIp
	 *            - whether or not this ip is private or public.
	 * @param endTime
	 *            .
	 * @throws TimeoutException .
	 * @throws MicrosoftAzureException .
	 * @throws InterruptedException .
	 */
	public void deleteVirtualMachineByIp(final String machineIp,
			final boolean isPrivateIp, final long endTime)
			throws TimeoutException, MicrosoftAzureException,
			InterruptedException {

		Deployment deployment = getDeploymentByIp(machineIp, isPrivateIp);
		if (deployment == null) {
			throw new MicrosoftAzureException("Could not find a Virtual Machine with IP " + machineIp);
		}
		logger.fine("Deployment name for Virtual Machine with IP " + machineIp + " is " + deployment.getName());
		deleteVirtualMachineByDeploymentName(deployment.getHostedServiceName(),
				deployment.getName(), endTime);

	}

	/**
	 * This method deletes the virtual machine under the deployment specifed by deploymentName.
	 * it also deletes the associated disk and cloud service.
	 * @param cloudServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @param endTime
	 *            .
	 * @throws TimeoutException .
	 * @throws MicrosoftAzureException .
	 * @throws InterruptedException .
	 */
	public void deleteVirtualMachineByDeploymentName(
			final String cloudServiceName, final String deploymentName,
			final long endTime) throws TimeoutException,
			MicrosoftAzureException, InterruptedException {

		String diskName = null;
		String roleName = null;
		Disk disk = getDiskByAttachedCloudService(cloudServiceName);
		if (disk != null) {
			diskName = disk.getName();
			roleName = disk.getAttachedTo().getRoleName();
		} else {
			throw new IllegalStateException("Disk cannot be null for an existing deployment " + deploymentName 
					+ " in cloud service " + cloudServiceName);
		}
		
		logger.info("Deleting Virtual Machine " + roleName);
		deleteDeployment(cloudServiceName, deploymentName,
				endTime);
		
		logger.fine("Deleting cloud service : " + cloudServiceName
				+ " that was dedicated for virtual machine " + roleName);				
		deleteCloudService(cloudServiceName, endTime);

		logger.fine("Waiting for OS Disk " + diskName + " to detach from role " + roleName);
		waitForDiskToDetach(diskName, roleName, endTime);
		logger.info("Deleting OS Disk : " + diskName);
		deleteOSDisk(diskName, endTime);
	}
	
	private Disk getDiskByAttachedCloudService(final String cloudServiceName) 
			throws MicrosoftAzureException, TimeoutException {
		
		Disks disks = listOSDisks();
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if ((attachedTo != null) && (attachedTo.getHostedServiceName().equals(cloudServiceName))) {
				return disk;
			}
		}
		return null;
	}

	private void waitForDiskToDetach(final String diskName, final String roleName, long endTime) 
			throws TimeoutException, MicrosoftAzureException, InterruptedException {
		
		while (true) {
			Disks disks = listOSDisks();
			Disk osDisk = null;
			for (Disk disk : disks) {
				if (disk.getName().equals(diskName)) {
					osDisk = disk;
					break;
				}
			}
			if (osDisk != null) {
				if (osDisk.getAttachedTo() == null) {
					return;
				} else {
					logger.fine("Disk " + diskName + " is still attached to role " + osDisk.getAttachedTo().getRoleName());
					Thread.sleep(DEFAULT_POLLING_INTERVAL);
				}
			} else {
				throw new MicrosoftAzureException("Disk " + diskName + " does not exist");
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for disk " + diskName + " to detach from role " + roleName);
			}
		}
		
	}

	/**
	 * this method return all disks that are currently being used by this
	 * subscription. NOTE : disks that are not attached to any deployment are
	 * also returned. this means that {@code Disk.getAttachedTo} might return
	 * null.
	 * 
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Disks listOSDisks() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/disks");
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		return (Disks) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 * This method deletes an OS disk with the specified name. or does
	 * nothing if the disk does not exist.
	 * @param diskName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteOSDisk(final String diskName, final long endTime)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {
		
		if (!osDiskExists(diskName)) {
			logger.info("OS Disk " + diskName + " does not exist");
			return true;
		}
		
		ClientResponse response = doDelete("/services/disks/" + diskName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;
	}

	/**
	 * This method deletes just the virtual machine from the specified cloud service.
	 * associated OS Disk and cloud service are not removed.
	 * @param hostedServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteDeployment(final String hostedServiceName,
			final String deploymentName, final long endTime)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {

		if (!deploymentExists(hostedServiceName, deploymentName)) {
			logger.info("Deployment " + deploymentName + " does not exist");
			return true;
		}
		
		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;

		logger.fine(getThreadIdentity() + "Waiting for pending request lock...");
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout,
				TimeUnit.MILLISECONDS);

		if (lockAcquired) {

			logger.fine(getThreadIdentity() + "Lock acquired : " + pendingRequest.hashCode());
			logger.fine(getThreadIdentity() + "Executing a request to delete virtual machine");

			try {

				logger.fine(getThreadIdentity() + "Deleting deployment of virtual machine from : "
						+ deploymentName);

				ClientResponse response = doDelete("/services/hostedservices/"
						+ hostedServiceName + "/deployments/" + deploymentName);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				pendingRequest.unlock();
				logger.fine(getThreadIdentity() + "Lock unlcoked");
			} catch (final Exception e) {
				logger.fine(getThreadIdentity() + "About to release lock " + pendingRequest.hashCode());
				pendingRequest.unlock();
				if (e instanceof MicrosoftAzureException) {
					throw (MicrosoftAzureException)e;
				}
				if (e instanceof TimeoutException) {
					throw (TimeoutException)e;
				}
				if (e instanceof InterruptedException) {
					throw (InterruptedException)e;
				}
			}
			return true;
		} else {
			throw new TimeoutException(
					"Failed to acquire lock for deleteDeployment request after + "
							+ lockTimeout + " milliseconds");
		}

	}

	/**
	 * 
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public HostedServices listHostedServices() throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doGet("/services/hostedservices");
		String responseBody = response.getEntity(String.class);
		checkForError(response);
		return (HostedServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 * 
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public AffinityGroups listAffinityGroups() throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doGet("/affinitygroups");
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		checkForError(response);
		return (AffinityGroups) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 * 
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public StorageServices listStorageServices()
			throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/storageservices");
		String responseBody = response.getEntity(String.class);
		return (StorageServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 * 
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public VirtualNetworkSites listVirtualNetworkSites()
			throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/networking/media");
		if (response.getStatus() == HTTP_NOT_FOUND) {
			return null;
		}
		String responseBody = response.getEntity(String.class);
		if ( responseBody.charAt(0) == BAD_CHAR) {
			responseBody = responseBody.substring(1);
		}

		GlobalNetworkConfiguration globalNetowrkConfiguration = (GlobalNetworkConfiguration) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return globalNetowrkConfiguration.getVirtualNetworkConfiguration()
				.getVirtualNetworkSites();

	}

	/**
	 * @param hostedServiceName
	 *            - hosted service name.
	 * @param embedDeployments
	 *            - whether or not to include the deployments of this hosted
	 *            service in the response.
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public HostedService getHostedService(final String hostedServiceName,
			final boolean embedDeployments) throws MicrosoftAzureException,
			TimeoutException {
		StringBuilder builder = new StringBuilder();
		builder.append("/services/hostedservices/").append(hostedServiceName);
		if (embedDeployments) {
			builder.append("?embed-detail=true");
		}
		ClientResponse response = doGet(builder.toString());
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		return (HostedService) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 * 
	 * @param hostedServiceName
	 *            .
	 * @param deploymentSlot
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByDeploymentSlot(
			final String hostedServiceName, final String deploymentSlot)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		try {
			response = doGet("/services/hostedservices/" + hostedServiceName
					+ "/deploymentslots/" + deploymentSlot);
			checkForError(response);
		} catch (TimeoutException e) {
			logger.warning("Timed out while waiting for deployment details. This may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		return (Deployment) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 * 
	 * @param hostedServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByDeploymentName(
			final String hostedServiceName, final String deploymentName)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		try {
			response = doGet("/services/hostedservices/" + hostedServiceName
					+ "/deployments/" + deploymentName);
			checkForError(response);
		} catch (TimeoutException e) {
			logger.warning("Timed out while waiting for deployment details. this may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		Deployment deployment = (Deployment) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		deployment.setHostedServiceName(hostedServiceName);
		return deployment;
	}

	/**
	 * 
	 * @param machineIp
	 *            .
	 * @param isPrivateIp
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByIp(final String machineIp,
			final boolean isPrivateIp) throws MicrosoftAzureException,
			TimeoutException {

		Deployment deployment = null;
		HostedServices cloudServices = listHostedServices();
		for (HostedService hostedService : cloudServices) {
			String cloudServiceName = hostedService.getServiceName();
			Deployments deployments = getHostedService(cloudServiceName, true)
					.getDeployments();
			// skip empty cloud services
			if (!deployments.getDeployments().isEmpty()) {
				deployment = deployments.getDeployments().get(0);
				String deploymentName = deployment.getName();
				deployment = getDeploymentByDeploymentName(cloudServiceName,
						deploymentName);
				String publicIp = getPublicIpFromDeployment(deployment);
				String privateIp = getPrivateIpFromDeployment(deployment);
				String ip = isPrivateIp ? privateIp : publicIp;
				if (machineIp.equals(ip)) {
					deployment.setHostedServiceName(cloudServiceName);
					return deployment;
				}
			}
		}
		logger.info("Could not find any roles with ip :" + machineIp);
		return null;

	}

	/**
	 * This method deletes the virtual network specified. or does 
	 * nothing if the virtual network does not exist.
	 * @param virtualNetworkSite
	 *            - virtual network site name to delete .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteVirtualNetworkSite(final String virtualNetworkSite,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		if (!virtualNetworkExists(virtualNetworkSite)) {
			return true;
		}
		VirtualNetworkSites virtualNetworkSites = listVirtualNetworkSites();
		int index = 0;
		for (int i = 0; i < virtualNetworkSites.getVirtualNetworkSites().size(); i++) {
			VirtualNetworkSite site = virtualNetworkSites
					.getVirtualNetworkSites().get(i);
			if (site.getName().equals(virtualNetworkSite)) {
				index = i;
				break;
			}
		}
		virtualNetworkSites.getVirtualNetworkSites().remove(index);
		logger.info("Deleting virtual network site : " + virtualNetworkSite);
		setNetworkConfiguration(endTime, virtualNetworkSites);
		logger.fine("Deleted virtual network site : " + virtualNetworkSite);
		return true;

	}

	private String extractRequestId(final ClientResponse response) {
		return response.getHeaders().getFirst("x-ms-request-id");
	}

	private ClientResponse doPut(final String url, final String body,
			final String contentType) throws MicrosoftAzureException {
		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, contentType)
				.put(ClientResponse.class, body);
		checkForError(response);
		return response;
	}

	private ClientResponse doPost(final String url, final String body)
			throws MicrosoftAzureException {

		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.post(ClientResponse.class, body);
		checkForError(response);
		return response;
	}

	private ClientResponse doGet(final String url)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		for (int i = 0; i < MAX_RETRIES; i++) {
			try {
				response = resource
						.path(subscriptionId + url)
						.header(X_MS_VERSION_HEADER_NAME,
								X_MS_VERSION_HEADER_VALUE)
						.header(CONTENT_TYPE_HEADER_NAME,
								CONTENT_TYPE_HEADER_VALUE)
						.get(ClientResponse.class);
				break;
			} catch (ClientHandlerException e) {
				logger.warning("Caught an exception while executing GET with url "
						+ url + ". Message :" + e.getMessage());
				logger.warning("Retrying request");
				continue;
			}
		}
		if (response == null) {
			throw new TimeoutException("Timed out while executing GET after "
					+ MAX_RETRIES);
		}

		return response;
	}

	private ClientResponse doDelete(final String url)
			throws MicrosoftAzureException {
		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.delete(ClientResponse.class);
		checkForError(response);
		return response;
	}

	private boolean cloudServiceExists(final String cloudServiceName) throws MicrosoftAzureException, TimeoutException {
		HostedServices cloudServices = listHostedServices();
		return (cloudServices.contains(cloudServiceName));
	}
	
	private boolean affinityExists(final String affinityGroupName)
			throws MicrosoftAzureException, TimeoutException {
		AffinityGroups affinityGroups = listAffinityGroups();
		return (affinityGroups.contains(affinityGroupName));
	}
	
	private boolean deploymentExists(final String cloudServiceName, final String deploymentName) 
			throws MicrosoftAzureException, TimeoutException {
		
		HostedService service = getHostedService(cloudServiceName, true);
		if ((service.getDeployments() != null) && (service.getDeployments().contains(deploymentName))) {
			return true;
		} else {
			return false;
		}
		
	}

	private boolean storageExists(final String storageAccouhtName)
			throws MicrosoftAzureException, TimeoutException {
		StorageServices storageServices = listStorageServices();
		return (storageServices.contains(storageAccouhtName));
	}
	
	private boolean osDiskExists(final String osDiskName) 
			throws MicrosoftAzureException, TimeoutException {
		Disks disks = listOSDisks();
		return (disks.contains(osDiskName));
	}
	
	private boolean virtualNetworkExists(final String virtualNetworkName) 
			throws MicrosoftAzureException, TimeoutException {
		VirtualNetworkSites sites = listVirtualNetworkSites();
		return (sites.contains(virtualNetworkName));
	}

	private void checkForError(final ClientResponse response)
			throws MicrosoftAzureException {
		int status = response.getStatus();
		if (status != HTTP_OK && status != HTTP_CREATED
				&& status != HTTP_ACCEPTED) { // we got some
			// sort of error
			Error error = (Error) MicrosoftAzureModelUtils.unmarshall(response
					.getEntity(String.class));
			String errorMessage = error.getMessage();
			String errorCode = error.getCode();
			throw new MicrosoftAzureException(errorCode,
					errorMessage);
		}
	}

	private Deployment waitForDeploymentStatus(final String state,
			final String hostedServiceName, final String deploymentSlot,
			final long endTime) throws TimeoutException,
			MicrosoftAzureException, InterruptedException {

		while (true) {
			Deployment deployment = getDeploymentByDeploymentSlot(
					hostedServiceName, deploymentSlot);
			String status = deployment.getStatus();
			if (status.equals(state)) {
				return deployment;
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Deployment waitForRoleInstanceStatus(final String state,
			final String hostedServiceName, final String deploymentSlot,
			final long endTime) throws TimeoutException,
			MicrosoftAzureException, InterruptedException {

		while (true) {
			Deployment deployment = getDeploymentByDeploymentSlot(
					hostedServiceName, deploymentSlot);
			String roleName = deployment.getRoleList().getRoles().get(0)
					.getRoleName();
			String status = deployment.getRoleInstanceList().getRoleInstances()
					.get(0).getInstanceStatus();
			boolean error = checkVirtualMachineStatusForError(status);
			if (error) {
				// bad status of VM.
				throw new MicrosoftAzureException("Virtual Machine " + roleName
						+ " was provisioned but found in status " + status);
			}
			if (status.equals(state)) {
				return deployment;
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}
			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			} 

		}

	}

	private void setNetworkConfiguration(final long endTime,
			final VirtualNetworkSites virtualNetworkSites)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {
		GlobalNetworkConfiguration networkConfiguration = requestBodyBuilder
				.buildGlobalNetworkConfiguration(virtualNetworkSites
						.getVirtualNetworkSites());

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				networkConfiguration, true);

		ClientResponse response = doPut("/services/networking/media",
				xmlRequest, "text/plain");
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
	}

	private void waitForRequestToFinish(final String requestId,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		while (true) {

			// Query Azure for operation details
			Operation operation = getOperation(requestId);
			String status = operation.getStatus();
			if (!status.equals(IN_PROGRESS)) {

				// if operation succeeded, we are good to go
				if (status.equals(SUCCEEDED)) {
					return;
				}
				if (status.equals(FAILED)) {
					String errorMessage = operation.getError().getMessage();
					String errorCode = operation.getError().getCode();
					throw new MicrosoftAzureException(errorCode, errorMessage);
				}
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Operation getOperation(final String requestId)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = doGet("/operations/" + requestId);
		return (Operation) MicrosoftAzureModelUtils
				.unmarshall(response.getEntity(String.class));
	}

	private String getPublicIpFromDeployment(final Deployment deployment) {
		ConfigurationSets configurationSets = deployment.getRoleList()
				.getRoles().get(0).getConfigurationSets();
		String publicIp = null;
		for (ConfigurationSet configurationSet : configurationSets) {
			if (configurationSet instanceof NetworkConfigurationSet) {
				NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
				publicIp = networkConfigurationSet.getInputEndpoints()
						.getInputEndpoints().get(0).getvIp();
			}
		}
		return publicIp;

	}

	private String getPrivateIpFromDeployment(final Deployment deployment) {
		return deployment.getRoleInstanceList().getRoleInstances().get(0)
				.getIpAddress();
	}
	
	private String getThreadIdentity() {
		String threadName = Thread.currentThread().getName();
		long threadId = Thread.currentThread().getId();
		return "[" + threadName + "]" + "[" + threadId + "] - ";
	}
}
