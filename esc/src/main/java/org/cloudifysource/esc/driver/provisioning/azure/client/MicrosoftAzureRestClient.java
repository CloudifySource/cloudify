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
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Error;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Operation;
import org.cloudifysource.esc.driver.provisioning.azure.model.PersistentVMRole;
import org.cloudifysource.esc.driver.provisioning.azure.model.RestartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.StartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**********************************************************************************
 * A REST client implementation for the Azure REST API. * Custom made for
 * consuming the IaaS deployment. * * this is still a work in progress. *
 * 
 * @author elip *
 **********************************************************************************/
public class MicrosoftAzureRestClient {

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
	private WebResource resource;
	private Client client;

	private String subscriptionId;

	private MicrosoftAzureSSLHelper sslHelper;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public MicrosoftAzureRestClient(final String subscriptionId,
			final String pathToPfx, final String pfxPassword) {
		this.subscriptionId = subscriptionId;
		this.init(pathToPfx, pfxPassword);
	}

	public MicrosoftAzureRestClient() {

	}

	private void init(final String pathToPfx, final String pfxPassword) {
		try {
			this.sslHelper = new MicrosoftAzureSSLHelper(pathToPfx, pfxPassword);
			this.client = createClient(sslHelper.createSSLContext());
			this.resource = client.resource(CORE_MANAGEMENT_END_POINT);
			this.requestBodyBuilder = new MicrosoftAzureRequestBodyBuilder();
		} catch (final Exception e) {
			throw new RuntimeException("Failed initializing rest client", e);
		}
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(final String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	/**
	 * 
	 * @param logger
	 *            - the logger to add to the client
	 */
	public void setLoggingFilter(final Logger logger) {
		this.client.addFilter(new LoggingFilter(logger));
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
	 * @param timeout
	 *            .
	 * @param timeunit
	 *            .
	 * @return - the newly created cloud service name.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public String createCloudService(final String affinityGroup,
			final long timeout, final TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {

		CreateHostedService createHostedService = requestBodyBuilder
				.buildCreateCloudService(affinityGroup);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createHostedService, false);

		ClientResponse response = doPost("/" + subscriptionId
				+ "/services/hostedservices", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
		return createHostedService.getServiceName();
	}

	/**
	 * this method creates a storage account with the given name, or does
	 * nothing if the account exists.
	 * 
	 * @param affinityGroup
	 *            - the affinity group for the storage account.
	 * @param storageAccountName
	 *            - the name for the storage account to create.
	 * @param timeout
	 *            .
	 * @param timeunit
	 *            .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createStorageAccount(final String affinityGroup,
			final String storageAccountName, final long timeout,
			final TimeUnit timeunit) throws MicrosoftAzureException,
			TimeoutException {

		CreateStorageServiceInput createStorageServiceInput = requestBodyBuilder
				.buildCreateStorageAccount(affinityGroup, storageAccountName);

		if (storageExists(storageAccountName)) {
			logger.fine("Found an existing storage account : "
					+ storageAccountName);
			return;
		}

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createStorageServiceInput, false);
		ClientResponse response = doPost("/" + subscriptionId
				+ "/services/storageservices", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
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
	 * @param networkName
	 *            - the name for the network to create
	 * @param timeout
	 *            .
	 * @param timeunit
	 *            .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createVirtualNetwork(final String addressSpace,
			final String affinityGroup, final String networkName,
			final long timeout, final TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {

		GlobalNetworkConfiguration networkConfiguration = requestBodyBuilder
				.buildGlobalNetworkConfiguration(addressSpace, affinityGroup,
						networkName);

		if (networkExists(networkName)) {
			logger.fine("Found an existing virtual netowrk : " + networkName);
			return;
		}

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				networkConfiguration, true);

		ClientResponse response = doPut("/" + subscriptionId
				+ "/services/networking/media", xmlRequest, "text/plain");
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
		logger.fine("created virtual network : " + networkName);
	}

	/**
	 * this method creates an affinity group with the given name, or does
	 * nothing if the group exists.
	 * 
	 * @param affinityGroup
	 *            - the name of the affinity group to create
	 * @param location
	 *            - one of MS Data Centers locations.
	 * @param timeout
	 *            .
	 * @param timeunit
	 *            .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createAffinityGroup(final String affinityGroup,
			final String location, final long timeout, final TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {

		CreateAffinityGroup createAffinityGroup = requestBodyBuilder
				.buildCreateAffinity(affinityGroup, location);

		if (affinityExists(affinityGroup)) {
			logger.fine("Found an existing affinity group : " + affinityGroup);
			return;
		}

		String xmlRequest = MicrosoftAzureModelUtils.marshall(
				createAffinityGroup, false);
		ClientResponse response = doPost("/" + subscriptionId
				+ "/affinitygroups", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
		logger.fine("created affinity group : " + affinityGroup);
	}

	/**
	 * this method creates a standalone virtual machine.
	 * 
	 * @param deplyomentDesc
	 *            .
	 * @param serviceName
	 *            - the cloud service to host the vm.
	 * @param timeout
	 *            .
	 * @param timeunit
	 *            .
	 * @return - an object that wraps the public and private ip of the vm.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public RoleAddressDetails createVirtualMachineDeployment(
			final CreatePersistentVMRoleDeploymentDescriptor deplyomentDesc,
			final String serviceName, final long timeout,
			final TimeUnit timeunit) throws MicrosoftAzureException,
			TimeoutException {

		Deployment deployment = requestBodyBuilder
				.buildDeployment(deplyomentDesc);

		String xmlRequest = MicrosoftAzureModelUtils
				.marshall(deployment, false);

		ClientResponse response = doPost("/" + subscriptionId
				+ "/services/hostedservices/" + serviceName + "/deployments",
				xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
		Deployment deploymentResponse = waitForDeploymentStatus("Running",
				serviceName, deployment.getDeploymentSlot(), timeout, timeunit);

		deploymentResponse = waitForRoleInstanceStatus("ReadyRole",
				serviceName, deployment.getDeploymentSlot(), timeout, timeunit);

		RoleAddressDetails roleAddressDetails = new RoleAddressDetails();
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
	 * @throws TimeoutException 
	 */
	public String listOsImages() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet(subscriptionId + "/services/images");
		return response.getEntity(String.class);

	}

	public String listDeployments(final String cloudServiceName,
			final String deploymentSlot) throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet(subscriptionId
				+ "/services/hostedservices/" + cloudServiceName
				+ "/deployments/" + deploymentSlot);
		return response.getEntity(String.class);
	}

	/**
	 * 
	 * @param storageAccountName
	 * @param timeout
	 * @param timeunit
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException
	 */
	public void deleteStorageAccount(final String storageAccountName,
			long timeout, TimeUnit timeunit) throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doDelete(subscriptionId
				+ "/services/storageservices/" + storageAccountName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	/**
	 * 
	 * @param cloudServiceName
	 * @param timeout
	 * @param timeunit
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException
	 */
	public void deleteCloudService(final String cloudServiceName, long timeout,
			TimeUnit timeunit) throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doDelete(subscriptionId
				+ "/services/hostedservices/" + cloudServiceName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	/**
	 * 
	 * @param diskName
	 * @param timeout
	 * @param timeunit
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException
	 */
	public void deleteOSDisk(final String diskName, long timeout,
			TimeUnit timeunit) throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doDelete(subscriptionId + "/services/disks/"
				+ diskName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	/**
	 * 
	 * @param cloudServiceName
	 * @param deploymentName
	 * @param roleName
	 * @param timeout
	 * @param timeunit
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException
	 */
	public void deleteRole(final String cloudServiceName,
			final String deploymentName, final String roleName,
			final long timeout, final TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doDelete(subscriptionId
				+ "/services/hostedservices/" + cloudServiceName
				+ "/deployments/" + deploymentName + "/roles/" + roleName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	public void deleteDeployment(final String hostedServiceName,
			final String deploymentName, long timeout, TimeUnit unit)
			throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doDelete(subscriptionId
				+ "/services/hostedservices/" + hostedServiceName
				+ "/deployments/" + deploymentName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, unit);
	}

	/**
	 * 
	 * @param serviceName
	 * @param deploymentName
	 * @param roleName
	 * @return
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException 
	 */
	public PersistentVMRole getRole(String serviceName, String deploymentName,
			String roleName) throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet(subscriptionId
				+ "/services/hostedservices/" + serviceName + "/deployments/"
				+ deploymentName + "/roles/" + roleName);
		String responseBody = response.getEntity(String.class);
		PersistentVMRole persistentVMRole = (PersistentVMRole) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return persistentVMRole;
	}

	/**
	 * 
	 * @return
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException 
	 */
	public String listLocations() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet(subscriptionId + "/locations");
		return response.getEntity(String.class);
	}

	public HostedServices listHostedServices() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet(subscriptionId
				+ "/services/hostedservices");
		String responseBody = response.getEntity(String.class);
		HostedServices hostedServices = (HostedServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return hostedServices;
	}

	public void restartRole(String serviceName, String deploymentName,
			String roleName, long timeout, TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {
		RestartRoleOperation operation = new RestartRoleOperation();
		String xmlRequest;
		try {
			xmlRequest = MicrosoftAzureModelUtils.marshall(operation, false);
		} catch (MicrosoftAzureException e) {
			throw new MicrosoftAzureException(e);
		}
		ClientResponse response = doPost(
				subscriptionId + "/services/hostedservices/" + serviceName
						+ "/deployments/" + deploymentName + "/roleinstances/"
						+ roleName + "/operations", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	public void startRole(String serviceName, String deploymentName,
			String roleName, long timeout, TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {
		StartRoleOperation operation = new StartRoleOperation();
		String xmlRequest;
		try {
			xmlRequest = MicrosoftAzureModelUtils.marshall(operation, false);
		} catch (MicrosoftAzureException e) {
			throw new MicrosoftAzureException(e);
		}
		ClientResponse response = doPost(
				subscriptionId + "/services/hostedservices/" + serviceName
						+ "/deployments/" + deploymentName + "/roleinstances/"
						+ roleName + "/operations", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, timeout, timeunit);
	}

	public AffinityGroups listAffinityGroups() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/" + subscriptionId
				+ "/affinitygroups");
		String responseBody = response.getEntity(String.class);

		AffinityGroups affinityGroups = (AffinityGroups) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return affinityGroups;
	}

	public StorageServices listStorageServices() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/" + subscriptionId
				+ "/services/storageservices");
		String responseBody = response.getEntity(String.class);
		StorageServices storageServices = (StorageServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return storageServices;
	}

	public VirtualNetworkSites listVirtualNetworkSites()
			throws MicrosoftAzureException {
		ClientResponse response = resource
				.path("/" + subscriptionId + "/services/networking/media")
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.get(ClientResponse.class);
		if (response.getStatus() == 404) {
			// exist
			return null;
		}
		String responseBody = response.getEntity(String.class);
		if ((int) responseBody.charAt(0) == 65279) {
			responseBody = responseBody.substring(1);
		}

		GlobalNetworkConfiguration globalNetowrkConfiguration = (GlobalNetworkConfiguration) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return globalNetowrkConfiguration.getVirtualNetworkConfiguration()
				.getVirtualNetworkSites();

	}

	private String extractRequestId(final ClientResponse response) {
		return response.getHeaders().getFirst("x-ms-request-id");
	}

	@SuppressWarnings("unused")
	private ClientResponse doPut(final String url, final String body)
			throws MicrosoftAzureException {
		return doPut(url, body, CONTENT_TYPE_HEADER_VALUE);
	}

	private ClientResponse doPut(final String url, final String body,
			final String contentType) throws MicrosoftAzureException {
		ClientResponse response = resource.path(url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, contentType)
				.put(ClientResponse.class, body);
		checkForError(response);
		return response;
	}

	private ClientResponse doPost(final String url, final String body)
			throws MicrosoftAzureException {
		ClientResponse response = resource.path(url)
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
						.path(url)
						.header(X_MS_VERSION_HEADER_NAME,
								X_MS_VERSION_HEADER_VALUE)
						.header(CONTENT_TYPE_HEADER_NAME,
								CONTENT_TYPE_HEADER_VALUE)
						.get(ClientResponse.class);
				checkForError(response);
				break;
			} catch (ClientHandlerException e) {
				logger.warning("Caught an exception while executing GET with url "
						+ url + ". Message :" + e.getMessage());
				logger.warning("retrying request");
				continue;
			}
		}
		if (response == null) {
			throw new TimeoutException("Timed out while executing GET after " + MAX_RETRIES);
		}
		
		return response;
	}

	private ClientResponse doDelete(final String url)
			throws MicrosoftAzureException {
		ClientResponse response = resource.path(url)
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

	private boolean networkExists(final String networkName)
			throws MicrosoftAzureException {
		VirtualNetworkSites virtualNetworkSites = listVirtualNetworkSites();
		if (virtualNetworkSites == null) {
			return false;
		}
		if (virtualNetworkSites.contains(networkName)) {
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

	private void checkForError(ClientResponse response)
			throws MicrosoftAzureException {
		int status = response.getStatus();
		if (status != 200 && status != 201 && status != 202) { // we got some
			// sort of error
			Error error = (Error) MicrosoftAzureModelUtils.unmarshall(response
					.getEntity(String.class));
			String errorMessage = error.getMessage();
			throw new MicrosoftAzureException(status + " : " + errorMessage);
		}
	}

	private Deployment waitForDeploymentStatus(String state,
			String hostedServiceName, String deploymentSlot, long timeout,
			TimeUnit timeunit) throws TimeoutException, MicrosoftAzureException {

		long endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
		while (true) {
			Deployment deployment = getDeployment(hostedServiceName,
					deploymentSlot);
			String status = deployment.getStatus();
			if (status.equals(state)) {
				return deployment;
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Deployment waitForRoleInstanceStatus(String state,
			String hostedServiceName, String deploymentSlot, long timeout,
			TimeUnit timeunit) throws TimeoutException, MicrosoftAzureException {

		long endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
		while (true) {
			Deployment deployment = getDeployment(hostedServiceName,
					deploymentSlot);
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
					deleteRole(hostedServiceName, deploymentName, roleName,
							timeout, timeunit);
				} catch (MicrosoftAzureException e) {
					logger.warning("Failed deleting role unhealthy role "
							+ roleName);
					throw e;
				}
			}
			if (status.equals(state)) {
				return deployment;
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	/**
	 * 
	 * @param hostedServiceName
	 * @param deploymentSlot
	 * @return
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException 
	 */
	public Deployment getDeployment(String hostedServiceName,
			String deploymentSlot) throws MicrosoftAzureException, TimeoutException {
		
		ClientResponse response = null;
		try {
			response = doGet("/" + subscriptionId
					+ "/services/hostedservices/" + hostedServiceName
					+ "/deploymentslots/" + deploymentSlot);
		} catch (TimeoutException e) {
			logger.warning("Timed out while waiting for deployment details. this may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		Deployment deployment = (Deployment) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return deployment;
	}

	private void waitForRequestToFinish(final String requestId,
			final long timeout, final TimeUnit timeunit)
			throws MicrosoftAzureException, TimeoutException {

		long endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
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
					throw new MicrosoftAzureException(errorMessage);
				}
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Operation getOperation(final String requestId)
			throws MicrosoftAzureException {
		ClientResponse response = resource
				.path("/" + subscriptionId + "/operations/" + requestId)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.get(ClientResponse.class);

		Operation operation = (Operation) MicrosoftAzureModelUtils
				.unmarshall(response.getEntity(String.class));
		return operation;
	}

	// private Operation newOperation(final String entity)
	// throws MicrosoftAzureException {
	//
	//
	// DocumentBuilder documentBuilder = createDocumentBuilder();
	// Document xmlDoc = null;
	// Operation operation = null;
	// try {
	// xmlDoc = documentBuilder.parse(new InputSource(new StringReader(
	// entity)));
	// XPath xpath = XPathFactory.newInstance().newXPath();
	// String id = xpath.evaluate("/Operation/ID", xmlDoc);
	// String status = xpath.evaluate("/Operation/Status", xmlDoc);
	// String statusCode = xpath.evaluate("/Operation/HttpStatusCode",
	// xmlDoc);
	// String errorMessage = xpath.evaluate("/Operation/Error/Message",
	// xmlDoc);
	//
	// Error error = null;
	// if (errorMessage != null && !errorMessage.isEmpty()) {
	// String errorCode = xpath.evaluate("/Operation/Error/Code",
	// xmlDoc);
	// error = new Error();
	// error.setCode(errorCode);
	// error.setMessage(errorMessage);
	// }
	//
	// operation = new Operation();
	// operation.setId(id);
	// operation.setStatus(status);
	// operation.setStatusCode(statusCode);
	// if (error != null) {
	// operation.setError(error);
	// }
	//
	// } catch (SAXException e) {
	// throw new MicrosoftAzureException(
	// "Failed to parse XML Response from server. Response was: "
	// + entity + ", Error was: " + e.getMessage(), e);
	// } catch (XPathExpressionException e) {
	// throw new MicrosoftAzureException(
	// "Failed to parse XML Response from server. Response was: "
	// + entity + ", Error was: " + e.getMessage(), e);
	// } catch (IOException e) {
	// throw new MicrosoftAzureException(
	// "Failed to parse XML Response from server. Response was: "
	// + entity + ", Error was: " + e.getMessage(), e);
	// }
	// return operation;
	// }
}
