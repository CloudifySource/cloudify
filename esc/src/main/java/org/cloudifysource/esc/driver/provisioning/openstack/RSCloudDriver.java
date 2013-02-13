package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;

/**************
 * A custom cloud driver for RackStack OpenStack, using keystone authentication.
 * 
 * @author yoramw
 * @since 2.1
 * 
 */
public class RSCloudDriver extends CloudDriverSupport implements ProvisioningDriver {

	private static final int HTTP_NOT_FOUND = 404;
	private static final int SERVER_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes
	private static final String OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT = "openstack.identity.endpoint";
	private static final String OPENSTACK_WIRE_LOG = "openstack.wireLog";
	private static final String RS_OPENSTACK_TENANT = "openstack.tenant";
	private static final String OPENSTACK_OPENSTACK_ENDPOINT = "openstack.endpoint";

	private final XPath xpath = XPathFactory.newInstance().newXPath();

	private final Client client;

	private String serverNamePrefix;
	private WebResource service;
	private String pathPrefix;
	private String identityEndpoint;

	private final Object xmlFactoryMutex = new Object();
	private final DocumentBuilderFactory dbf;

	/************
	 * Constructor.
	 * 
	 * @throws ParserConfigurationException
	 */
	public RSCloudDriver() {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);

		final ClientConfig config = new DefaultClientConfig();
		Client httpClient = Client.create(config);
		httpClient.setConnectTimeout(CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		httpClient.setReadTimeout(CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		this.client = httpClient;

	}

	private DocumentBuilder createDocumentBuilder() {
		synchronized (xmlFactoryMutex) {
			// Document builder is not guaranteed to be thread sage
			try {
				// Document builders are not thread safe
				return dbf.newDocumentBuilder();
			} catch (final ParserConfigurationException e) {
				throw new IllegalStateException("Failed to set up XML Parser", e);
			}
		}

	}

	@Override
	public void close() {
	}

	@Override
	public String getCloudName() {
		return "rsopenstack";
	}

	@Override
	public void setConfig(final Cloud cloud, final String templateName,
			final boolean management, final String serviceName) {
		super.setConfig(
				cloud, templateName, management, serviceName);

		validateCloudConfig();
		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup();
		} else {
			this.serverNamePrefix = this.cloud.getProvider().getMachineNamePrefix();
		}

		String tenant = (String) this.cloud.getCustom().get(RS_OPENSTACK_TENANT);
		if (tenant == null) {
			throw new IllegalArgumentException("Custom field '" + RS_OPENSTACK_TENANT + "' must be set");
		}

		this.pathPrefix = "/v1.0/" + tenant + "/";

		String endpoint = (String) this.cloud.getCustom().get(OPENSTACK_OPENSTACK_ENDPOINT);
		if (endpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_ENDPOINT + "' must be set");
		}
		this.service = client.resource(endpoint);

		this.identityEndpoint = (String) this.cloud.getCustom().get(
				OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT);
		if (this.identityEndpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT
					+ "' must be set");
		}

		final String wireLog = (String) this.cloud.getCustom().get(
				OPENSTACK_WIRE_LOG);
		if (wireLog != null) {
			if (Boolean.parseBoolean(wireLog)) {
				this.client.addFilter(new LoggingFilter(logger));
			}
		}

	}

	private void validateCloudConfig() {
		String managementMachineNamePrefix = this.cloud.getProvider().getManagementGroup();
		if (managementMachineNamePrefix.contains("_")) {
			throw new IllegalArgumentException("The '_' character is not allowed in the attribute "
					+ "'managementGroup' for the rackspace cloud-driver as it is not supported in the rackspace cloud");
		}
		String agentMachineNamePrefix = this.cloud.getProvider().getMachineNamePrefix();
		if (agentMachineNamePrefix.contains("_")) {
			throw new IllegalArgumentException(
					"The '_' character is not allowed in the attribute "
							+ "'machineNamePrefix' for the rackspace cloud-driver as it " 
							+ "is not supported in the rackspace cloud");
		}
	}

	@Override
	public MachineDetails startMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		final String token = createAuthenticationToken();
		MachineDetails md;
		try {
			md = newServer(
					token, endTime, this.template);
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
		return md;
	}

	private long calcEndTimeInMillis(final long duration, final TimeUnit unit) {
		return System.currentTimeMillis() + unit.toMillis(duration);
	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		final String token = createAuthenticationToken();
		final long endTime = calcEndTimeInMillis(
				duration, unit);

		final int numOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();

		// thread pool - one per machine
		final ExecutorService executor =
				Executors.newFixedThreadPool(numOfManagementMachines);

		try {
			return doStartManagement(
					endTime, token, numOfManagementMachines, executor);
		} finally {
			executor.shutdown();
		}
	}

	private MachineDetails[] doStartManagement(final long endTime, final String token,
			final int numOfManagementMachines, final ExecutorService executor)
			throws CloudProvisioningException {

		// launch machine on a thread
		final List<Future<MachineDetails>> list = new ArrayList<Future<MachineDetails>>(numOfManagementMachines);
		for (int i = 0; i < numOfManagementMachines; ++i) {
			final Future<MachineDetails> task = executor.submit(new Callable<MachineDetails>() {

				@Override
				public MachineDetails call()
						throws Exception {

					final MachineDetails md = newServer(
							token, endTime, template);
					return md;

				}

			});
			list.add(task);

		}

		// get the machines
		Exception firstException = null;
		final List<MachineDetails> machines = new ArrayList<MachineDetails>(numOfManagementMachines);
		for (final Future<MachineDetails> future : list) {
			try {
				machines.add(future.get());
			} catch (final Exception e) {
				if (firstException == null) {
					firstException = e;
				}
			}
		}

		if (firstException == null) {
			return machines.toArray(new MachineDetails[machines.size()]);
		} else {
			// in case of an exception, clear the machines
			logger.warning("Provisioning of management machines failed, the following node will be shut down: "
					+ machines);
			for (final MachineDetails machineDetails : machines) {
				try {
					this.terminateServer(
							machineDetails.getMachineId(), token, endTime);
				} catch (final Exception e) {
					logger.log(
							Level.SEVERE,
							"While shutting down machine after provisioning of management machines failed, "
									+ "shutdown of node: " + machineDetails.getMachineId()
									+ " failed. This machine may be leaking. Error was: " + e.getMessage(), e);
				}
			}

			throw new CloudProvisioningException(
					"Failed to launch management machines: " + firstException.getMessage(), firstException);
		}
	}

	@Override
	public boolean stopMachine(final String ip, final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {
		final long endTime = calcEndTimeInMillis(
				duration, unit);

		if (isStopRequestRecent(ip)) {
			return false;
		}

		final String token = createAuthenticationToken();

		try {
			terminateServerByIp(
					ip, token, endTime);
			return true;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {
		final String token = createAuthenticationToken();

		final long endTime = calcEndTimeInMillis(
				DEFAULT_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		List<Node> nodes;
		try {
			nodes = listServers(token);
		} catch (final OpenstackException e) {
			throw new CloudProvisioningException(e);
		}

		final List<String> ids = new LinkedList<String>();
		for (final Node node : nodes) {
			if (node.getName().startsWith(
					this.serverNamePrefix)) {
				try {
					ids.add(node.getId());

				} catch (final Exception e) {
					throw new CloudProvisioningException(e);
				}
			}
		}

		try {
			terminateServers(
					ids, token, endTime);
		} catch (final TimeoutException e) {
			throw e;
		} catch (final Exception e) {
			throw new CloudProvisioningException("Failed to shut down managememnt machines", e);
		}
	}

	private Node getNode(final String nodeId, final String token)
			throws OpenstackException {
		final String response = service.path(
				this.pathPrefix + "servers/" + nodeId).
				queryParam("dummyReq", Long.toString(System.currentTimeMillis())).
				header("X-Auth-Token", token).accept(
						MediaType.APPLICATION_XML).get(
						String.class);
		final Node node = new Node();
		try {
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			node.setId(xpath.evaluate(
					"/server/@id", xmlDoc));
			node.setStatus(xpath.evaluate(
					"/server/@status", xmlDoc));
			node.setName(xpath.evaluate(
					"/server/@name", xmlDoc));

			NodeList addresses = (NodeList) xpath.evaluate(
					"/server/addresses/private/ip/@addr", xmlDoc, XPathConstants.NODESET);

			if (addresses.getLength() > 0) {
				node.setPrivateIp(addresses.item(
						0).getTextContent());

			}

			addresses = (NodeList) xpath.evaluate(
					"/server/addresses/public/ip/@addr", xmlDoc, XPathConstants.NODESET);
			if (addresses.getLength() > 0) {
				node.setPublicIp(addresses.item(
						0).getTextContent());
			}
		} catch (XPathExpressionException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		}

		return node;

	}

	List<Node> listServers(final String token)
			throws OpenstackException {
		final List<String> ids = listServerIds(token);
		final List<Node> nodes = new ArrayList<Node>(ids.size());

		for (final String id : ids) {
			try {
				Node node = getNode(id, token);
				nodes.add(node);
			} catch (UniformInterfaceException e) {
				if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
					// list servers may return servers that are shutting down.
					// ignore those who are deleted at this point
				} else {
					throw e;
				}
				
			} catch (OpenstackException e) {
				//Do nothing.
			}
		}

		return nodes;
	}

	private List<String> listServerIds(final String token)
			throws OpenstackException {

		String response = null;
		try {
			response = service.path(
					this.pathPrefix + "servers")
					.queryParam("dummyReq", Long.toString(System.currentTimeMillis()))
					.header(
							"X-Auth-Token", token).accept(
							MediaType.APPLICATION_XML).get(
							String.class);

			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			final NodeList idNodes = (NodeList) xpath.evaluate(
					"/servers/server/@id", xmlDoc, XPathConstants.NODESET);
			final int howmany = idNodes.getLength();
			final List<String> ids = new ArrayList<String>(howmany);
			for (int i = 0; i < howmany; i++) {
				ids.add(idNodes.item(
						i).getTextContent());

			}
			return ids;

		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity);

		} catch (SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (XPathException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);

		}
	}

	private void terminateServerByIp(final String serverIp, final String token, final long endTime)
			throws Exception {
		final Node node = getNodeByIp(
				serverIp, token);
		if (node == null) {
			throw new IllegalArgumentException("Could not find a server with IP: " + serverIp);
		}
		terminateServer(
				node.getId(), token, endTime);
	}

	private Node getNodeByIp(final String serverIp, final String token)
			throws OpenstackException {
		final List<Node> nodes = listServers(token);
		logger.log(Level.INFO, "Looking for node ip " + serverIp);
		for (final Node node : nodes) {
			logger.log(Level.INFO, "node private ip is " + node.getPrivateIp());
			logger.log(Level.INFO, "node public ip is " + node.getPublicIp());
			if ((node.getPrivateIp() != null && node.getPrivateIp().equalsIgnoreCase(serverIp))
					|| (node.getPublicIp() != null && node.getPublicIp().equalsIgnoreCase(serverIp))) {
				return node;
			}
		}

		return null;
	}

	private void terminateServer(final String serverId, final String token, final long endTime)
			throws Exception {
		terminateServers(
				Arrays.asList(serverId), token, endTime);
	}

	private void terminateServers(final List<String> serverIds, final String token, final long endTime)
			throws Exception {

		for (final String serverId : serverIds) {
			try {
				service.path(
						this.pathPrefix + "servers/" + serverId).header(
						"X-Auth-Token", token).accept(
						MediaType.APPLICATION_XML).delete();
			} catch (final UniformInterfaceException e) {
				final String responseEntity = e.getResponse().getEntity(String.class);
				throw new IllegalArgumentException(e + " Response entity: " + responseEntity);
			}

		}

		int successCounter = 0;

		// wait for all servers to die
		for (final String serverId : serverIds) {
			while (System.currentTimeMillis() < endTime) {
				try {
					this.getNode(
							serverId, token);

				} catch (final UniformInterfaceException e) {
					if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
						++successCounter;
						break;
					}
					throw e;
				}
				Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);
			}

		}

		if (successCounter == serverIds.size()) {
			return;
		}

		throw new TimeoutException("Nodes " + serverIds + " did not shut down in the required time");

	}

	/**
	 * Creates server. Block until complete. Returns id
	 * 
	 * @param name the server name
	 * @param timeout the timeout in seconds
	 * @param serverTemplate the cloud template to use for this server
	 * @return the server id
	 */
	private MachineDetails newServer(final String token, final long endTime, final ComputeTemplate serverTemplate)
			throws Exception {

		final MachineDetails md = createServer(
				token, serverTemplate);

		try {
			// wait until complete
			waitForServerToReachStatus(
					md, endTime, md.getMachineId(), token, "ACTIVE");

			md.setAgentRunning(false);
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(serverTemplate.getRemoteDirectory());

			return md;
		} catch (final Exception e) {
			logger.log(
					Level.WARNING, "server: " + md.getMachineId() + " failed to start up correctly. "
							+ "Shutting it down. Error was: " + e.getMessage(), e);
			try {
				terminateServer(
						md.getMachineId(), token, endTime);
			} catch (final Exception e2) {
				logger.log(
						Level.WARNING,
						"Error while shutting down failed machine: " + md.getMachineId()
								+ ". Error was: " + e.getMessage()
								+ ".It may be leaking.", e);
			}
			throw e;
		}

	}

	private MachineDetails createServer(final String token, final ComputeTemplate serverTemplate)
			throws OpenstackException {
		final String serverName = this.serverNamePrefix + System.currentTimeMillis();
		// Start the machine!
		final String json =
				"{\"server\":{ \"name\":\"" + serverName + "\",\"imageId\":" + serverTemplate.getImageId()
						+ ",\"flavorId\":" + serverTemplate.getHardwareId() + "}}";

		String serverBootResponse = null;
		try {
			serverBootResponse = service.path(
					this.pathPrefix + "servers")
					.header(
							"Content-Type", "application/json").header(
							"X-Auth-Token", token).accept(
							MediaType.APPLICATION_XML).post(
							String.class, json);
		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity);
		}

		try {
			// if we are here, the machine started!
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document doc = documentBuilder.parse(new InputSource(new StringReader(serverBootResponse)));

			final String status = xpath.evaluate(
					"/server/@status", doc);
			if (!status.startsWith("BUILD")) {
				throw new IllegalStateException("Expected server status of BUILD(*), got: " + status);
			}

			final String serverId = xpath.evaluate(
					"/server/@id", doc);
			final String rootPassword = xpath.evaluate(
					"/server/@adminPass", doc);
			MachineDetails md = new MachineDetails();
			md.setMachineId(serverId);
			md.setRemoteUsername(serverTemplate.getUsername());
			md.setRemotePassword(rootPassword);
			return md;
		} catch (XPathExpressionException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + serverBootResponse
					+ ", Error was: " + e.getMessage(), e);
		}
	}

	private void waitForServerToReachStatus(final MachineDetails md, final long endTime, final String serverId,
			final String token, final String status)
			throws OpenstackException, TimeoutException, InterruptedException {

		final String respone = null;
		while (true) {

			final Node node = this.getNode(
					serverId, token);

			final String currentStatus = node.getStatus().toLowerCase();

			if (currentStatus.equalsIgnoreCase(status)) {

				md.setPrivateAddress(node.getPrivateIp());
				md.setPublicAddress(node.getPublicIp());
				break;
			} 
			if (currentStatus.contains("error")) {
				throw new OpenstackException("Server provisioning failed. Node ID: " + node.getId() + ", status: "
						+ node.getStatus());
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("timeout creating server. last status:" + respone);
			}

			Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);

		}

	}

	/**********
	 * Creates an openstack keystone authentication token.
	 * 
	 * @return the authentication token.
	 */

	public String createAuthenticationToken() {
		final String json =
				"{\"auth\":{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"" + this.cloud.getUser().getUser()
						+ "\",\"apiKey\":\"" + this.cloud.getUser().getApiKey() + "\"}}}";
		final WebResource service = client.resource(this.identityEndpoint);
		final String resp = service.path(
				"/v2.0/tokens").header(
				"Content-Type", "application/json").post(
				String.class, json);

		return getAutenticationTokenIdFromResponse(resp);
	}

	@SuppressWarnings("unchecked")
	private String getAutenticationTokenIdFromResponse(final String resp) {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			Map<String, Object> readValue = mapper.readValue(new StringReader(resp), Map.class);
			Map<String, Object> accessMap = (Map<String, Object>) readValue.get("access");
			Map<String, String> tokenMap = (Map<String, String>) accessMap.get("token");
			return tokenMap.get("id");
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object getComputeContext() {
		return null;
	}
}
