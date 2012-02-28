package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
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
 * A custom cloud driver for OpenStack, using keystone authentication.
 * 
 * @author barakme
 * @since 2.1
 * 
 */
public class OpenstackCloudDriver extends CloudDriverSupport implements ProvisioningDriver {

	private static final int SERVER_POLLING_INTERVAL_MILLIS = 1000;
	private static final int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes
	private static final String HP_ALLOCATE_FLOATING_IP = "hp.allocate-floating-ip";
	private static final String HP_OPENSTACK_IDENTITY_ENDPOINT = "hp.openstack.identity.endpoint";
	private static final String HP_WIRE_LOG = "hp.wireLog";
	private static final String HP_KEY_PAIR = "hp.keyPair";
	private static final String HP_SECURITYGROUP = "hp.securityGroup";
	private static final String HP_OPENSTACK_ENDPOINT = "hp.openstack.endpoint";
	private static final String HP_TENANT = "hp.tenant";

	private final XPath xpath = XPathFactory.newInstance().newXPath();
	// private Admin admin = null;

	private final DocumentBuilder documentBuilder;
	private final Client client;

	private String serverNamePrefix;
	private String tenant;
	private String endpoint;
	private WebResource service;
	private String pathPrefix;
	private String identityEndpoint;

	/************
	 * Constructor.
	 * 
	 * @throws ParserConfigurationException
	 */
	public OpenstackCloudDriver() {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		try {
			this.documentBuilder = dbf.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException("Failed to set up XML Parser", e);
		}

		final ClientConfig config = new DefaultClientConfig();
		this.client = Client.create(config);

	}

	@Override
	public void close() {
	}

	@Override
	public String getCloudName() {
		return "openstack";
	}

	@Override
	public void setConfig(final Cloud cloud, final String templateName, final boolean management) {
		super.setConfig(cloud, templateName, management);

		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup();
		} else {
			this.serverNamePrefix = this.cloud.getProvider().getMachineNamePrefix();
		}

		this.tenant = (String) this.cloud.getCustom().get(HP_TENANT);
		if (tenant == null) {
			throw new IllegalArgumentException("Custom field '" + HP_TENANT + "' must be set");
		}

		this.pathPrefix = "v1.1/" + tenant + "/";

		this.endpoint = (String) this.cloud.getCustom().get(HP_OPENSTACK_ENDPOINT);
		if (this.endpoint == null) {
			throw new IllegalArgumentException("Custom field '" + HP_OPENSTACK_ENDPOINT + "' must be set");
		}
		this.service = client.resource(this.endpoint);

		this.identityEndpoint = (String) this.cloud.getCustom().get(HP_OPENSTACK_IDENTITY_ENDPOINT);
		if (this.identityEndpoint == null) {
			throw new IllegalArgumentException("Custom field '" + HP_OPENSTACK_IDENTITY_ENDPOINT + "' must be set");
		}

		final String wireLog = (String) this.cloud.getCustom().get(HP_WIRE_LOG);
		if (wireLog != null) {
			if (Boolean.parseBoolean(wireLog)) {
				this.client.addFilter(new LoggingFilter(logger));
			}
		}

	}

	@Override
	public MachineDetails startMachine(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		final String token = createAuthenticationToken();
		MachineDetails md;
		try {
			md = newServer(token, endTime, this.template);
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
		return md;
	}

	private long calcEndTimeInMillis(final long duration, final TimeUnit unit) {
		return System.currentTimeMillis() + unit.toMillis(duration);
	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {
		final String token = createAuthenticationToken();
		final long endTime = calcEndTimeInMillis(duration, unit);

		final int numOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();

		// thread pool - one per machine
		final ExecutorService executor = Executors.newFixedThreadPool(cloud.getProvider()
				.getNumberOfManagementMachines());

		try {
			return doStartManagement(endTime, token, numOfManagementMachines, executor);
		} finally {
			executor.shutdown();
		}
	}

	private MachineDetails[] doStartManagement(final long endTime, final String token,
			final int numOfManagementMachines, final ExecutorService executor) throws CloudProvisioningException {

		// launch machine on a thread
		final List<Future<MachineDetails>> list = new ArrayList<Future<MachineDetails>>(numOfManagementMachines);
		for (int i = 0; i < numOfManagementMachines; ++i) {
			final Future<MachineDetails> task = executor.submit(new Callable<MachineDetails>() {

				@Override
				public MachineDetails call() throws Exception {

					final MachineDetails md = newServer(token, endTime, template);
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
					this.terminateServer(machineDetails.getMachineId(), token, endTime);
				} catch (final Exception e) {
					logger.log(Level.SEVERE,
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
	public boolean stopMachine(final String ip, final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException, CloudProvisioningException {
		final long endTime = calcEndTimeInMillis(duration, unit);

		if (isStopRequestRecent(ip)) {
			return false;
		}

		final String token = createAuthenticationToken();

		try {
			terminateServerByIp(ip, token, endTime);
			return true;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {
		final String token = createAuthenticationToken();

		final long endTime = calcEndTimeInMillis(DEFAULT_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		List<Node> nodes;
		try {
			nodes = listServers(token);
		} catch (final XPathExpressionException e) {
			throw new CloudProvisioningException(e);
		} catch (final SAXException e) {
			throw new CloudProvisioningException(e);
		} catch (final IOException e) {
			throw new CloudProvisioningException(e);
		}

		final List<String> ids = new LinkedList<String>();
		for (final Node node : nodes) {
			if (node.getName().startsWith(this.serverNamePrefix)) {
				try {
					ids.add(node.getId());

				} catch (final Exception e) {
					throw new CloudProvisioningException(e);
				}
			}
		}

		try {
			terminateServers(ids, token, endTime);
		} catch (TimeoutException e) {
			throw e;
		} catch (Exception e) {
			throw new CloudProvisioningException("Failed to shut down managememnt machines", e);
		}
	}

	private Node getNode(final String nodeId, final String token) throws XPathExpressionException, SAXException,
			IOException {
		final String response = service.path(this.pathPrefix + "servers/" + nodeId).header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_XML).get(String.class);

		final Document xmlDoc = this.documentBuilder.parse(new InputSource(new StringReader(response)));

		final Node node = new Node();
		node.setId(xpath.evaluate("/server/@id", xmlDoc));
		node.setStatus(xpath.evaluate("/server/@status", xmlDoc));
		node.setName(xpath.evaluate("/server/@name", xmlDoc));

		final NodeList addresses = (NodeList) xpath.evaluate("/server/addresses/network/ip/@addr", xmlDoc,
				XPathConstants.NODESET);

		if (addresses.getLength() > 0) {
			node.setPrivateIp(addresses.item(0).getTextContent());

		}

		if (addresses.getLength() > 1) {
			node.setPublicIp(addresses.item(1).getTextContent());
		}

		return node;

	}

	List<Node> listServers(final String token) throws XPathExpressionException, SAXException, IOException {
		final List<String> ids = listServerIds(token);
		final List<Node> nodes = new ArrayList<Node>(ids.size());

		for (final String id : ids) {
			nodes.add(getNode(id, token));
		}

		return nodes;
	}

	// public void listFlavors(final String token) throws Exception {
	// final WebResource service = client.resource(this.endpoint);
	//
	// String response = null;
	//
	// response = service.path(this.pathPrefix + "flavors").header("X-Auth-Token", token)
	// .accept(MediaType.APPLICATION_XML).get(String.class);
	//
	// System.out.println(response);
	//
	// }

	private List<String> listServerIds(final String token) throws SAXException, IOException, XPathExpressionException {

		String response = null;
		try {
			response = service.path(this.pathPrefix + "servers").header("X-Auth-Token", token)
					.accept(MediaType.APPLICATION_XML).get(String.class);

			final Document xmlDoc = this.documentBuilder.parse(new InputSource(new StringReader(response)));

			final NodeList idNodes = (NodeList) xpath.evaluate("/servers/server/@id", xmlDoc, XPathConstants.NODESET);
			final int howmany = idNodes.getLength();
			final List<String> ids = new ArrayList<String>(howmany);
			for (int i = 0; i < howmany; i++) {
				ids.add(idNodes.item(i).getTextContent());

			}
			return ids;

		} catch (final UniformInterfaceException e) {
			throw new IOException(e);

		}
	}

	private void terminateServerByIp(final String serverIp, final String token, final long endTime) throws Exception {
		final Node node = getNodeByIp(serverIp, token);
		if (node == null) {
			throw new IllegalArgumentException("Could not find a server with IP: " + serverIp);
		}
		terminateServer(node.getId(), token, endTime);
	}

	private Node getNodeByIp(final String serverIp, final String token) throws XPathExpressionException, SAXException,
			IOException {
		final List<Node> nodes = listServers(token);
		for (final Node node : nodes) {
			if (node.getPrivateIp() != null && node.getPrivateIp().equals(serverIp) || node.getPublicIp() != null
					&& node.getPublicIp().equals(serverIp)) {
				return node;
			}
		}

		return null;
	}

	private void terminateServer(final String serverId, final String token, final long endTime) throws Exception {
		terminateServers(Arrays.asList(serverId), token, endTime);
	}

	private void terminateServers(final List<String> serverIds, final String token, final long endTime)
			throws Exception {

		// detach public ip and delete the servers
		for (String serverId : serverIds) {

			final Node node = getNode(serverId, token);
			if (node.getPublicIp() != null) {
				detachFloatingIP(serverId, node.getPublicIp(), token);
				deleteFloatingIP(node.getPublicIp(), token);
			}
			try {
				service.path(this.pathPrefix + "servers/" + serverId).header("X-Auth-Token", token)
						.accept(MediaType.APPLICATION_XML).delete();
			} catch (final UniformInterfaceException e) {
				throw new IllegalArgumentException(e);
			}

		}

		int successCounter = 0;

		// wait for all servers to die
		for (String serverId : serverIds) {
			while (System.currentTimeMillis() < endTime) {
				try {
					Node n = this.getNode(serverId, token);
					// System.out.println("Server: " + serverId + " Status: " + n.getStatus());

				} catch (UniformInterfaceException e) {
					if (e.getResponse().getStatus() == 404) {
						++successCounter;
						break;
					}
					throw e;
				}

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
	 * @param name
	 *            the server name
	 * @param timeout
	 *            the timeout in seconds
	 * @param serverTemplate
	 *            the cloud template to use for this server
	 * @return the server id
	 */
	private MachineDetails newServer(final String token, final long endTime, final CloudTemplate serverTemplate)
			throws Exception {

		// The result machine details
		final MachineDetails md = new MachineDetails();

		final String serverName = this.serverNamePrefix + System.currentTimeMillis();
		final String securityGroup = getCustomTemplateValue(serverTemplate, HP_SECURITYGROUP, null, false);
		final String keyPairName = getCustomTemplateValue(serverTemplate, HP_KEY_PAIR, null, false);

		// Start the machine!
		final String json = "{\"server\":{ \"name\":\"" + serverName + "\",\"imageRef\":\""
				+ serverTemplate.getImageId() + "\",\"flavorRef\":\"" + serverTemplate.getHardwareId()
				+ "\",\"key_name\":\"" + keyPairName + "\",\"security_groups\":[{\"name\":\"" + securityGroup
				+ "\"}]}}";

		String serverBootResponse = null;
		try {
			serverBootResponse = service.path(this.pathPrefix + "servers").header("Content-Type", "application/json")
					.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML).post(String.class, json);
		} catch (final UniformInterfaceException e) {
			throw new IllegalStateException(e);
		}

		// if we are here, the machine started!
		final Document doc = documentBuilder.parse(new InputSource(new StringReader(serverBootResponse)));

		final String status = xpath.evaluate("/server/@status", doc);
		if (!status.startsWith("BUILD")) {
			throw new IllegalStateException("Expected server status of BUILD(*), got: " + status);
		}

		final String serverId = xpath.evaluate("/server/@id", doc);

		try {
			// wait until complete
			waitForServerToReachStatus(md, endTime, serverId, token, "ACTIVE");

			final String allocateIp = (String) serverTemplate.getOptions().get(HP_ALLOCATE_FLOATING_IP);
			if (allocateIp == null || Boolean.parseBoolean(allocateIp)) {
				// if (this.management) {
				// attach floating ip
				final String floatingIp = allocateFloatingIP(token);

				addFloatingIP(String.valueOf(serverId), floatingIp, token);
				md.setPublicAddress(floatingIp);
			}
			// }

			md.setMachineId(serverId);
			md.setAgentRunning(false);
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(cloud.getProvider().getRemoteDirectory());

			md.setRemoteUsername("root");

			return md;
		} catch (final Exception e) {
			logger.log(Level.WARNING, "server: " + serverId + " failed to start up correctly. "
					+ "Shutting it down. Error was: " + e.getMessage(), e);
			try {
				terminateServer(serverId, token, endTime);
			} catch (final Exception e2) {
				logger.log(Level.WARNING,
						"Error while shutting down failed machine: " + serverId + ". Error was: " + e.getMessage()
								+ ".It may be leaking.", e);
			}
			throw e;
		}

	}

	private String getCustomTemplateValue(final CloudTemplate serverTemplate, final String key,
			final String defaultValue, final boolean allowNull) {
		final String value = (String) serverTemplate.getOptions().get(key);
		if (value == null) {
			if (allowNull) {
				return defaultValue;
			} else {
				throw new IllegalArgumentException("Template option '" + key + "' must be set");
			}
		} else {
			return value;
		}

	}

	private void waitForServerToReachStatus(final MachineDetails md, final long endTime, final String serverId,
			final String token, final String status) throws Exception {

		final String respone = null;
		while (true) {
			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("timeout creating server. last status:" + respone);
			}
			Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);

			final Node node = this.getNode(serverId, token);

			if (node.getStatus().equalsIgnoreCase(status)) {

				md.setPrivateAddress(node.getPrivateIp());
				break;
			}

		}

	}

	@SuppressWarnings("rawtypes")
	List<FloatingIP> listFloatingIPs(final String token) throws SAXException, IOException {
		final String response = service.path(this.pathPrefix + "os-floating-ips").header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_JSON).get(String.class);

		final ObjectMapper mapper = new ObjectMapper();
		final Map map = mapper.readValue(new StringReader(response), Map.class);
		@SuppressWarnings("unchecked")
		final List<Map> list = (List<Map>) map.get("floating_ips");
		final List<FloatingIP> floatingIps = new ArrayList<FloatingIP>(map.size());

		for (final Map floatingIpMap : list) {
			final FloatingIP ip = new FloatingIP();

			final Object instanceId = floatingIpMap.get("instance_id");

			ip.setInstanceId(instanceId == null ? null : instanceId.toString());
			ip.setIp((String) floatingIpMap.get("ip"));
			ip.setFixedIp((String) floatingIpMap.get("fixed_ip"));
			ip.setId(floatingIpMap.get("id").toString());
			floatingIps.add(ip);
		}
		return floatingIps;

	}

	private FloatingIP getFloatingIpByIp(final String ip, final String token) throws SAXException, IOException {
		final List<FloatingIP> allips = listFloatingIPs(token);
		for (final FloatingIP floatingIP : allips) {
			if (ip.equals(floatingIP.getIp())) {
				return floatingIP;
			}
		}

		return null;
	}

	private void deleteFloatingIP(final String ip, final String token) throws SAXException, IOException {

		final FloatingIP floatingIp = getFloatingIpByIp(ip, token);
		if (floatingIp == null) {
			logger.warning("Could not find floating IP " + ip + " in list. IP was not deleted.");
		} else {
			service.path(this.pathPrefix + "os-floating-ips/" + floatingIp.getId()).header("X-Auth-Token", token)
					.accept(MediaType.APPLICATION_JSON).delete();

		}

	}

	private String allocateFloatingIP(final String token) {

		try {
			final String resp = service.path(this.pathPrefix + "os-floating-ips")
					.header("Content-type", "application/json").header("X-Auth-Token", token)
					.accept(MediaType.APPLICATION_JSON).post(String.class, "");

			final Matcher m = Pattern.compile("\"ip\": \"([^\"]*)\"").matcher(resp);
			if (m.find()) {
				return m.group(1);
			} else {
				throw new IllegalStateException("Failed to allocate floating IP - IP not found in response");
			}
		} catch (final UniformInterfaceException e) {
			logRestError(e);
			throw new IllegalStateException("Failed to allocate floating IP", e);
		}

	}

	private void logRestError(final UniformInterfaceException e) {
		logger.severe("REST Error: " + e.getMessage());
		logger.severe("REST Status: " + e.getResponse().getStatus());
		logger.severe("REST Message: " + e.getResponse().getEntity(String.class));
	}

	/**
	 * Attaches a previously allocated floating ip to a server.
	 * 
	 * @param serverid
	 * @param ip
	 *            public ip to be assigned
	 * @param token
	 * @throws Exception
	 */
	private void addFloatingIP(final String serverid, final String ip, final String token) throws Exception {

		service.path(this.pathPrefix + "servers/" + serverid + "/action")
				.header("Content-type", "application/json")
				.header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_JSON)
				.post(String.class,
						String.format("{\"addFloatingIp\":{\"server\":\"%s\",\"address\":\"%s\"}}", serverid, ip));

	}

	private void detachFloatingIP(final String serverId, final String ip, final String token) {

		service.path(this.pathPrefix + "servers/" + serverId + "/action")
				.header("Content-type", "application/json")
				.header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_JSON)
				.post(String.class,
						String.format("{\"removeFloatingIp\":{\"server\": \"%s\", \"address\": \"%s\"}}", serverId, ip));

	}

	public static void main(final String[] args) throws Exception {

		// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// dbf.setNamespaceAware(false);
		// DocumentBuilder db = dbf.newDocumentBuilder();
		// final XPath xpath = XPathFactory.newInstance().newXPath();
		// final String xml =
		// "<server accessIPv4=\"\" accessIPv6=\"\" adminPass=\"A4kSBxrwR6Fn7htV\" created=\"2012-02-16T12:52:01Z\" hostId=\"\" id=\"42133\" name=\"y6\" status=\"BUILD(scheduling)\" tenantId=\"24912589714038\" updated=\"2012-02-16T12:52:01Z\" userId=\"98173213380893\" uuid=\"82f2ef97-0674-4d63-a30a-2e72300e1914\" xmlns=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:atom=\"http://www.w3.org/2005/Atom\"> "
		// +
		// "<atom:link href=\"https://az-2.region-a.geo-1.compute.hpcloudsvc.com/v1.1/24912589714038/servers/42133\" rel=\"self\"/> "
		// +
		// " <atom:link href=\"https://az-2.region-a.geo-1.compute.hpcloudsvc.com/24912589714038/servers/42133\" rel=\"bookmark\"/>"
		// + "<image id=\"221\">"
		// +
		// "    <atom:link href=\"https://az-2.region-a.geo-1.compute.hpcloudsvc.com/24912589714038/images/221\" rel=\"bookmark\"/>"
		// + "</image>"
		// + "<flavor id=\"100\">"
		// +
		// "    <atom:link href=\"https://az-2.region-a.geo-1.compute.hpcloudsvc.com/24912589714038/flavors/100\" rel=\"bookmark\"/>"
		// + "</flavor>" + "<addresses/>" + "</server>";
		//
		//
		// Document doc = db.parse(new InputSource(new StringReader(xml)));
		//
		// final Object s = xpath.evaluate("/server/@status", doc);
		//

		// new CloudDriver().newServer("y10", 3600);
		final OpenstackCloudDriver cd = new OpenstackCloudDriver();
		final DSLReader reader = new DSLReader();
		reader.setDslFile(new File(
				"D:/Gigaspaces/gigaspaces-cloudify-2.0.1-m1/tools/cli/plugins/esc/hp/hp-cloud.groovy"));
		reader.setCreateServiceContext(false);
		final Cloud cloud = reader.readDslEntity(Cloud.class);
		cd.setConfig(cloud, "SMALL_LINUX_32", true);
		final String token = cd.createAuthenticationToken();

		// final List<Node> servers = cd.listServers(token);
		// for (final Node node : servers) {
		// cd.terminateServer(node.id, token);
		// }

		final List<FloatingIP> ips = cd.listFloatingIPs(token);
		System.out.println(ips);
		// for (final FloatingIP floatingIP : ips) {
		// cd.deleteFloatingIP(floatingIP.ip, token);
		//
		// }

		// cd.listFloatingIPs(token);

		// final int numOfIpsBefore = cd.listFloatingIPs(token).size();
		// final MachineDetails server = cd.newServer(token, 300, cloud.getTemplates().values().iterator().next());
		// cd.terminateServer(server.getMachineId(), token);
		// final int numOfIpsAfter = cd.listFloatingIPs(token).size();
		// System.out.println("Before: " + numOfIpsBefore + ", after: " + numOfIpsAfter);
		// // final String ip = cd.allocateFloatingIP(token);
		// final List<Node> nodes = cd.listServers(token);
		// for (Node node : nodes) {
		// if(node.publicIp != null && node.publicIp.equals("15.185.175.193")) {
		//
		// }
		// }
		// final String id = nodes.iterator().next().id;
		//
		// cd.addFloatingIP(id, ip, token);
		// cd.detachFloatingIP(id, ip, token);
		// cd.listFlavors(token);
		// // final List<String> ids = cd.listServerIds(token);
		// for (final String id : ids) {
		// final Node node = cd.getNode(id, token);
		// System.out.println(node);
		// cd.terminateServer(id, token);
		// }

	}

	public String createAuthenticationToken() {

		final String json = "{\"auth\":{\"apiAccessKeyCredentials\":{\"accessKey\":\"" + this.cloud.getUser().getUser()
				+ "\",\"secretKey\":\"" + this.cloud.getUser().getApiKey() + "\"},\"tenantId\":\"" + this.tenant
				+ "\"}}";

		final WebResource service = client.resource(this.identityEndpoint);

		final String resp = service.path("/v2.0/tokens").header("Content-Type", "application/json")
				.accept(MediaType.APPLICATION_XML).post(String.class, json);

		final Matcher m = Pattern.compile("token id=\"([^\"]*)\"").matcher(resp);
		if (m.find()) {
			final String token = m.group(1);
			return token;
		}

		throw new RuntimeException("error:" + resp);
	}
}
