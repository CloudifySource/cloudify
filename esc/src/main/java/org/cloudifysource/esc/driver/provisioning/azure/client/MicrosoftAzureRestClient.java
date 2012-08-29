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
	private static final String X_MS_VERSION_HEADER_VALUE = "2012-03-01";
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
		Client client = Client.create(config);
		return client;
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

		logger.fine("[" + Thread.currentThread().getName() + "] - "
				+ "creating cloud cervice");

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
			logger.fine("[" + Thread.currentThread().getName()
					+ "] - cloud cervice created : " + serviceName);
		} catch (final Exception e) {
			logger.warning("failed to create cloud service " + e.getMessage());
			if (e instanceof MicrosoftAzureException) {
				throw new MicrosoftAzureException(e);
			}
			if (e instanceof TimeoutException) {
				throw new TimeoutException(e.getMessage());
			}
			if (e instanceof InterruptedException) {
				throw new InterruptedException(e.getMessage());
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
			logger.fine("Found an existing storage account : "
					+ storageAccountName);
			return;
		}

		logger.fine("creating a storage account : " + storageAccountName);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createStorageServiceInput, false);
		ClientResponse response = doPost("/services/storageservices",
				xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);

		logger.fine("created a storage account : " + storageAccountName);

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
			logger.fine("Found an existing virtual netowrk site : "
					+ networkSiteName);
			return;
		} else {
			if (virtualNetworkSites == null) {
				virtualNetworkSites = new VirtualNetworkSites();
			}
		}

		logger.fine("creating virtual network site : " + networkSiteName);

		VirtualNetworkSite newSite = new VirtualNetworkSite();
		AddressSpace address = new AddressSpace();
		address.setAddressPrefix(addressSpace);
		newSite.setAddressSpace(address);
		newSite.setAffinityGroup(affinityGroup);
		newSite.setName(networkSiteName);

		virtualNetworkSites.getVirtualNetworkSites().add(newSite);

		setNetworkConfiguration(endTime, virtualNetworkSites);
		logger.fine("created virtual network site : " + networkSiteName);
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
			logger.fine("Found an existing affinity group : " + affinityGroup);
			return;
		}

		logger.fine("creating affinity group : " + affinityGroup);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createAffinityGroup, false);
		ClientResponse response = doPost("/affinitygroups", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("created affinity group : " + affinityGroup);
	}

	/**
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
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis
				- ESTIMATED_TIME_TO_START_VM;
		if (lockTimeout < 0) {
			throw new MicrosoftAzureException(
					"aborted request to provision virtual machine. "
							+ "the timeout is less then the estimated time to provision the machine");
		}

		logger.fine("[" + Thread.currentThread().getName()
				+ "] - waiting for pending request lock...");
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout,
				TimeUnit.MILLISECONDS);

		String serviceName = null;
		Deployment deployment = null;

		if (lockAcquired) {

			logger.fine("[" + Thread.currentThread().getName()
					+ "] - lock acquired");
			logger.fine("["
					+ Thread.currentThread().getName()
					+ "] - executing a request to provision a new virtual machine");

			try {

				serviceName = createCloudService(
						deplyomentDesc.getAffinityGroup(), endTime);

				deplyomentDesc.setHostedServiceName(serviceName);
				deplyomentDesc.setDeploymentName(serviceName);

				deployment = requestBodyBuilder.buildDeployment(deplyomentDesc);

				String xmlRequest = MicrosoftAzureModelUtils.marshall(
						deployment, false);

				logger.fine("[" + Thread.currentThread().getName() + "] - "
						+ "launching virtual machine : "
						+ deplyomentDesc.getRoleName());

				ClientResponse response = doPost("/services/hostedservices/"
						+ serviceName + "/deployments", xmlRequest);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				pendingRequest.unlock();
				logger.fine("[" + Thread.currentThread().getName()
						+ "] - lock unlcoked");
			} catch (final Exception e) {
				pendingRequest.unlock();
				logger.fine("[" + Thread.currentThread().getName()
						+ "] - lock unlcoked");
				if (e instanceof MicrosoftAzureException) {
					throw new MicrosoftAzureException(e);
				}
				if (e instanceof TimeoutException) {
					throw new TimeoutException(e.getMessage());
				}
				if (e instanceof InterruptedException) {
					throw new InterruptedException(e.getMessage());
				}
			}
		} else {
			throw new TimeoutException(
					"failed to acquire lock for deleteDeployment request after + "
							+ lockTimeout + " milliseconds");
		}

		Deployment deploymentResponse = waitForDeploymentStatus("Running",
				serviceName, deployment.getDeploymentSlot(), endTime);

		deploymentResponse = waitForRoleInstanceStatus("ReadyRole",
				serviceName, deployment.getDeploymentSlot(), endTime);

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
		if (vmStatus.equals("FailedStartingRole")
				|| vmStatus.equals("FailedStartingVM")
				|| vmStatus.equals("UnresponsiveRole")
				|| vmStatus.equals("CyclingRole")) {
			return true;
		}
		return false;

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
	 * 
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
		ClientResponse response = doDelete("/services/storageservices/"
				+ storageAccountName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;

	}

	/**
	 * 
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
		ClientResponse response = doDelete("/affinitygroups/"
				+ affinityGroupName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;
	}

	/**
	 * 
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
		deleteVirtualMachineByDeploymentName(deployment.getHostedServiceName(),
				deployment.getName(), endTime);

	}

	/**
	 * 
	 * @param cloudServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @param endTime
	 *            .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public void deleteVirtualMachineByDeploymentIfExists(
			final String cloudServiceName, final String deploymentName,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {
		HostedService service = getHostedService(cloudServiceName, true);
		Deployments deployments = service.getDeployments();
		if (deployments.contains(deploymentName)) {
			deleteVirtualMachineByDeploymentName(cloudServiceName,
					deploymentName, endTime);
		}
	}

	/**
	 * 
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

		try {

			Disks disks = listOSDisks();
			for (Disk disk : disks) {
				AttachedTo attachedTo = disk.getAttachedTo();
				if (attachedTo != null) {
					if (cloudServiceName.equals(attachedTo
							.getHostedServiceName())) {
						diskName = disk.getName();
						roleName = attachedTo.getRoleName();
						break;
					}
				}
			}

			deleteDeployment(cloudServiceName, deploymentName,
					endTime);

			logger.fine("deleting os disk : " + diskName
					+ " that belonged to the virtual machine " + roleName);

			deleteOSDisk(diskName, endTime);

			logger.fine("deleteing cloud service : " + cloudServiceName
					+ " that was dedicated for virtual machine " + roleName);

			deleteCloudService(cloudServiceName, endTime);

		} catch (final MicrosoftAzureException e) { 
			logger.severe(ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final TimeoutException e) {
			logger.severe(ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final InterruptedException e) {
			logger.severe(ExceptionUtils.getFullStackTrace(e));
			throw e;
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
		Disks disks = (Disks) MicrosoftAzureModelUtils.unmarshall(responseBody);
		return disks;
	}

	/**
	 * 
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
		ClientResponse response = doDelete("/services/disks/" + diskName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;
	}

	/**
	 * 
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

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;

		logger.fine("[" + Thread.currentThread().getName()
				+ "] - waiting for pending request lock...");
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout,
				TimeUnit.MILLISECONDS);

		if (lockAcquired) {

			logger.fine("[" + Thread.currentThread().getName()
					+ "] - lock acquired");
			logger.fine("["
					+ Thread.currentThread().getName()
					+ "] - executing a request to delete virtual machine");

			try {

				logger.fine("[" + Thread.currentThread().getName()
						+ "] - deleting deployment of virtual machine from : "
						+ deploymentName);

				ClientResponse response = doDelete("/services/hostedservices/"
						+ hostedServiceName + "/deployments/" + deploymentName);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				pendingRequest.unlock();
				logger.fine("[" + Thread.currentThread().getName()
						+ "] - lock unlcoked");
			} catch (final Exception e) {
				logger.warning("["
						+ Thread.currentThread().getName()
						+ "] failed deleting deployment of virtual machine from : "
						+ deploymentName);
				pendingRequest.unlock();
				logger.fine("[" + Thread.currentThread().getName()
						+ "] - lock unlcoked");
				if (e instanceof MicrosoftAzureException) {
					throw new MicrosoftAzureException(e);
				}
				if (e instanceof TimeoutException) {
					throw new TimeoutException(e.getMessage());
				}
				if (e instanceof InterruptedException) {
					throw new InterruptedException(e.getMessage());
				}
			}
			return true;
		} else {
			throw new TimeoutException(
					"failed to acquire lock for deleteDeployment request after + "
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
		HostedServices hostedServices = (HostedServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return hostedServices;
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
		AffinityGroups affinityGroups = (AffinityGroups) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return affinityGroups;
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
		StorageServices storageServices = (StorageServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return storageServices;
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
		builder.append("/services/hostedservices/" + hostedServiceName);
		if (embedDeployments) {
			builder.append("?embed-detail=true");
		}
		ClientResponse response = doGet(builder.toString());
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		HostedService hostedService = (HostedService) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return hostedService;
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
			logger.warning("Timed out while waiting for deployment details. this may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		Deployment deployment = (Deployment) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return deployment;
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
			deployment = getHostedService(cloudServiceName, true)
					.getDeployments().getDeployments().get(0);
			String deploymentName = deployment.getName();
			deployment = getDeploymentByDeploymentName(cloudServiceName,
					deploymentName);
			String publicIp = getPublicIpFromDeployment(deployment);
			String privateIp = getPrivateIpFromDeployment(deployment);
			String ip = isPrivateIp ? privateIp : publicIp;
			if (machineIp.equals(ip)) {
				deployment.setHostedServiceName(cloudServiceName);
				logger.info("found a role with ip : " + machineIp);
				return deployment;
			}
		}
		logger.info("could not find any roles with ip :" + machineIp);
		return null;

	}

	/**
	 * 
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
		setNetworkConfiguration(endTime, virtualNetworkSites);
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
				logger.warning("retrying request");
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

	private boolean affinityExists(final String affinityGroupName)
			throws MicrosoftAzureException, TimeoutException {
		AffinityGroups affinityGroups = listAffinityGroups();
		if (affinityGroups.contains(affinityGroupName)) {
			return true;
		}
		return false;
	}

	private boolean storageExists(final String storageAccouhtName)
			throws MicrosoftAzureException, TimeoutException {
		StorageServices storageServices = listStorageServices();
		if (storageServices.contains(storageAccouhtName)) {
			return true;
		}
		return false;
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
			throw new MicrosoftAzureException(String.valueOf(status),
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
			String deploymentName = deployment.getName();
			String status = deployment.getRoleInstanceList().getRoleInstances()
					.get(0).getInstanceStatus();
			boolean error = checkVirtualMachineStatusForError(status);
			if (error) {
				logger.warning("Virtual Machine " + roleName
						+ " was provisioned but found in status " + status);
				logger.warning("This role is unhealthy, deleting it");
				try {
					deleteDeployment(hostedServiceName, deploymentName, endTime);
				} catch (MicrosoftAzureException e) {
					logger.warning("Failed deleting role unhealthy role "
							+ roleName);
					throw e;
				}
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
					String errorStatus = operation.getStatus();
					throw new MicrosoftAzureException(errorStatus, errorMessage);
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

		// ClientResponse response = resource
		// .path("/" + subscriptionId + "/operations/" + requestId)
		// .header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
		// .get(ClientResponse.class);

		Operation operation = (Operation) MicrosoftAzureModelUtils
				.unmarshall(response.getEntity(String.class));
		return operation;
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
}
