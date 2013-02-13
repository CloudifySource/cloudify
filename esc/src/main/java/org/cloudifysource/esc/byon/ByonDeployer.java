/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.esc.byon;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.byon.CustomNodeImpl;
import org.cloudifysource.esc.util.IPUtils;

/**
 * @author noak
 * @since 2.0.1
 *
 *        Implements a cloud-simulator, using private machines as a pool of
 *        nodes on which the application is deployed. The list of available
 *        nodes and matching credentials are configured through the cloud Groovy
 *        file.
 */
public class ByonDeployer {

	private static final String PROVIDER_ID = "BYON";
	private static final String NODES_LIST_FREE = "FREE";
	private static final String NODES_LIST_ALLOCATED = "ALLOCATED";
	private static final String NODES_LIST_INVALID = "INVALID";
	private static final String NODES_LIST_TERMINATED = "TERMINATED";
	private static final String CLOUD_NODE_ID = "id";
	private static final String CLOUD_NODE_HOST_LIST = "host-list";
	private static final String CLOUD_NODE_HOST_RANGE = "host-range";
	private static final String CLOUD_NODE_USERNAME = "username";
	private static final String CLOUD_NODE_CREDENTIAL = "credential";

	private static final String INVALID_HOSTS_ERR_MESSAGE = "Failed to start cloud node, invalid hosts configuration.";
	private static final String EMPTY_HOSTS_ERR_MESSAGE = "Failed to start cloud node, empty ID configuration";
	private static final String EMPTY_IP_RANGE_ERR_MESSAGE = "Failed to start cloud node, invalid IP range "
			+ "configuration: missing \"-\"";

	protected static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ByonDeployer.class.getName());

	private final Map<String, Map<String, List<CustomNode>>> nodesListsByTemplates = new Hashtable<String, Map<String, List<CustomNode>>>();

	/**
	 * Constructor.
	 */
	public ByonDeployer() {
	}

	/**
	 * Adds a list of nodes related to a specific template.
	 *
	 * @param templateName
	 *            The name of the template this nodes-list belongs to
	 * @param template
	 * 				The cloud template for this node list.
	 * @param nodesList
	 *            A list of maps, each map representing a cloud node
	 * @throws Exception
	 *             Indicates the node parsing failed
	 */
	public synchronized void addNodesList(final String templateName,
			final ComputeTemplate template,
			final List<Map<String, String>> nodesList) throws Exception {
		final List<CustomNode> resolvedNodes = new ArrayList<CustomNode>();
		final List<CustomNode> unresolvedNodes = new ArrayList<CustomNode>();
		logger.info("addNodesList: adding node list of " + templateName
				+ " to ByonDeployer, node list: " + nodesList);

		// parse the given nodes list
		List<CustomNode> parsedNodes = parseCloudNodes(nodesList);
		parsedNodes = removeDuplicates(parsedNodes);

		// the infrastructure is based on machine IPs, they need to be unique.
		// we set the resolved IP address on each node for an easy machine
		// comparison from this point on
		for (CustomNode node : parsedNodes) {
			try {
				node.setResolvedIP(IPUtils.resolveHostName(node.getPrivateIP()));
				if (template.getRemoteExecution() == RemoteExecutionModes.WINRM) {
					node.setLoginPort(RemoteExecutionModes.WINRM.getDefaultPort());
				}
				IPUtils.validateConnection(node.getResolvedIP(),
						node.getLoginPort());
				resolvedNodes.add(node);
			} catch (final Exception ex) {
				// this node is not reachable - add it to the invalid nodes pool
				unresolvedNodes.add(node);
			}
		}

		// avoid duplicate machines in different templates (compare by IP)
		final Set<String> duplicateNodes = getDuplicateIPs(getAllNodes(),
				parsedNodes);
		if (duplicateNodes.size() > 0) {
			throw new CloudProvisioningException(
					"Failed to add nodes for template \""
							+ templateName
							+ "\","
							+ " some IP addresses were already defined by a different template: "
							+ Arrays.toString(duplicateNodes.toArray()));
		}

		setInitialPoolsForTemplate(templateName, resolvedNodes, unresolvedNodes);
	}

	/**
	 * Creates a server (AKA a machine or a node) with the assigned logical
	 * name. The server is taken from the list of free nodes, unless this list
	 * is exhausted. If all there are no free nodes available, the invalid nodes
	 * are checked for SSH connection, and if a connection can be established -
	 * the node is used.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverName
	 *            A logical name used to uniquely identify this node (does not
	 *            have to match the host name)
	 * @return A node available for use
	 * @throws CloudProvisioningException
	 *             Indicated a new machine could not be allocated, either
	 *             because the name is empty or because the nodes pool is
	 *             exhausted
	 */
	public synchronized CustomNode createServer(final String templateName,
			final String serverName) throws CloudProvisioningException {

		if (org.apache.commons.lang.StringUtils.isBlank(serverName)) {
			throw new CloudProvisioningException(
					"Failed to create new cloud node, server name is missing");
		}

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to create new cloud node. \"" + templateName
					+ "\" is not a known template.");
		}
		final List<CustomNode> freeNodesPool = templateLists
				.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists
				.get(NODES_LIST_ALLOCATED);
		final List<CustomNode> invalidNodesPool = templateLists
				.get(NODES_LIST_INVALID);

		if (freeNodesPool.isEmpty() && invalidNodesPool.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to create a new cloud node for template \""
							+ templateName
							+ "\", all available nodes are currently used");
		}

		CustomNode node = null;
		if (!freeNodesPool.isEmpty()) {
			node = freeNodesPool.iterator().next();
			// if this node is indeed not allocated, test the connectivity to
			// it.
			// if we can't connect to it - move it to the invalid pool,
			// otherwise - allocate it.
			if (!allocatedNodesPool.contains(node)) {
				try {
					// verify this node is still resolvable and reachable
					String resolvedIP = IPUtils.resolveHostName(node
							.getPrivateIP());
					node.setResolvedIP(resolvedIP);
					IPUtils.validateConnection(node.getResolvedIP(),
							node.getLoginPort());
					allocatedNodesPool.add(node);
					freeNodesPool.remove(node);
				} catch (final Exception e) {
					// catch any exception - to prevent a machine leak. Add the
					// machine to the invalids pool
					logger.log(
							Level.INFO,
							"Failed to create server on " + node.getPrivateIP()
							+ ", connection failed on port "
							+ node.getLoginPort(), e);
					try {
						invalidateServer(templateName, node);
					} catch (final CloudProvisioningException ie) {
						logger.log(Level.INFO,
								"Failed to mark machine " + node.getPrivateIP()
								+ " as Invalid.", ie);
					}
					throw new CloudProvisioningException(e);
				}
			}
		} else {
            for (CustomNode currentNode : invalidNodesPool) {
                try {
                    String resolvedIP = IPUtils.resolveHostName(currentNode.getPrivateIP());
                    currentNode.setResolvedIP(resolvedIP);
                    IPUtils.validateConnection(currentNode.getPrivateIP(), currentNode.getLoginPort());
                    if (!allocatedNodesPool.contains(currentNode)) {
                        allocatedNodesPool.add(currentNode);
                    }
                    invalidNodesPool.remove(currentNode);
                    node = currentNode;
                    break;
                } catch (final Exception ex) {
                        // ignore and continue
                }
            }
		}

		if (node == null) {
			throw new CloudProvisioningException(
					"Failed to create a new cloud node for template \""
							+ templateName
							+ "\", all available nodes are currently used");
		}

		node.setNodeName(serverName);

		return node;
	}

	/**
	 * Sets the servers with the specified IPs as allocated, so they would not
	 * be re-allocated on future calls.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template the IPs belongs to
	 * @param ipAddresses
	 *            A set of IP addresses (decimal dotted format)
	 *
	 * @throws CloudProvisioningException
	 *             Indicates the IPs could not be marked as allocated with the
	 *             specified template
	 */
	public synchronized void setAllocated(final String templateName,
			final Set<String> ipAddresses) throws CloudProvisioningException {
		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to set allocated servers. \"" + templateName
					+ "\" is not a known template.");
		}
		final List<CustomNode> freeNodesPool = templateLists
				.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists
				.get(NODES_LIST_ALLOCATED);

		for (final String ipAddress : ipAddresses) {
			for (final CustomNode node : freeNodesPool) {
				if (StringUtils.isNotBlank(node.getResolvedIP())
						&& node.getResolvedIP().equalsIgnoreCase(ipAddress)) {
					freeNodesPool.remove(node);
					if (!allocatedNodesPool.contains(node)) {
						allocatedNodesPool.add(node);
					}
					break;
				}
			}
		}
	}

	/**
	 * Shuts down a given node, (moves the node back to the free nodes list, to
	 * be used again).
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverName
	 *            A server to shutdown
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be shutdown with the specified
	 *             template
	 */
	public synchronized void shutdownServer(final String templateName,
			final CustomNode serverName) throws CloudProvisioningException {
		if (serverName == null) {
			return;
		}

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException("Failed to shutdown server \""
					+ serverName + "\". \"" + templateName
					+ "\" is not a known template.");
		}
		final List<CustomNode> freeNodesPool = templateLists
				.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists
				.get(NODES_LIST_ALLOCATED);

		((CustomNodeImpl) serverName).setGroup(null);
		allocatedNodesPool.remove(serverName);
		if (!freeNodesPool.contains(serverName)) {
			freeNodesPool.add(serverName);
		}
	}

	/**
	 * Shuts down the server with the given ID.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverId
	 *            The ID of the server to shutdown
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be shutdown with the specified
	 *             template
	 */
	public void shutdownServerById(final String templateName,
			final String serverId) throws CloudProvisioningException {
		shutdownServer(templateName, getServerByID(templateName, serverId));
	}

	/**
	 * Shuts down the server with the given IP address.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverIp
	 *            The IP of the server to shutdown (dotted decimal format)
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be shutdown with the specified
	 *             template
	 */
	public void shutdownServerByIp(final String templateName,
			final String serverIp) throws CloudProvisioningException {
		shutdownServer(templateName, getServerByIP(templateName, serverIp));
	}

	/**
	 * Retrieves the server with the given name (not a host name).
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverName
	 *            The name of the server to retrieve
	 * @return A node matching the given name, if found
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be obtained with the specified
	 *             template
	 */
	public CustomNode getServerByName(final String templateName,
			final String serverName) throws CloudProvisioningException {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodesByTemplateName(templateName)) {
			if (node.getNodeName().equalsIgnoreCase(serverName)) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	/**
	 * Retrieves the server with the given Id (not a host name).
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param id
	 *            The id of the server to retrieve
	 * @return A node matching the given id, if found
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be obtained with the specified
	 *             template
	 */
	public CustomNode getServerByID(final String templateName, final String id)
			throws CloudProvisioningException {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodesByTemplateName(templateName)) {
			if (node.getId().equalsIgnoreCase(id)) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	/**
	 * Retrieves the server with the given IP (not a host name).
	 *
	 * @param templateName
	 *            The name of the nodes-list' template this IP belongs to
	 * @param ipAddress
	 *            The IP address of the server to retrieve
	 * @return A node with the given IP, if found
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be obtained with the specified
	 *             template
	 */
	public CustomNode getServerByIP(final String templateName,
			final String ipAddress) throws CloudProvisioningException {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodesByTemplateName(templateName)) {
			if ((StringUtils.isNotBlank(node.getResolvedIP()) && node
					.getResolvedIP().equalsIgnoreCase(ipAddress))
					|| (StringUtils.isNotBlank(node.getPrivateIP()) && node
							.getPrivateIP().equalsIgnoreCase(ipAddress))) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	/**
	 * Retrieves all nodes (i.e. in all states - free, allocated and invalid).
	 *
	 * @param templateName
	 *            The name of the nodes-list' template to use
	 * @return A collection of all the managed nodes of the specified template
	 * @throws CloudProvisioningException
	 *             Indicates the servers list could not be obtained for the
	 *             given template name
	 */
	public Set<CustomNode> getAllNodesByTemplateName(final String templateName)
			throws CloudProvisioningException {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to get servers list. \"" + templateName
					+ "\" is not a known template.");
		}
		final List<CustomNode> freeNodesPool = templateLists
				.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists
				.get(NODES_LIST_ALLOCATED);
		final List<CustomNode> invalidNodesPool = templateLists
				.get(NODES_LIST_INVALID);

		allNodes.addAll(freeNodesPool);
		allNodes.addAll(allocatedNodesPool);
		allNodes.addAll(invalidNodesPool);

		return allNodes;
	}

	/**
	 * Retrieves all free nodes.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template to use
	 * @return A collection of all the free nodes of the specified template
	 * @throws CloudProvisioningException
	 *             Indicates the servers list could not be obtained for the
	 *             given template name
	 */
	public Set<CustomNode> getFreeNodesByTemplateName(final String templateName)
			throws CloudProvisioningException {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to get servers list. \"" + templateName
					+ "\" is not a known template.");
		}

		allNodes.addAll(templateLists.get(NODES_LIST_FREE));

		return allNodes;
	}

	/**
	 * Retrieves all allocated nodes.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template to use
	 * @return A collection of all the allocated (active, in use) nodes of the
	 *         specified template
	 * @throws CloudProvisioningException
	 *             Indicates the servers list could not be obtained for the
	 *             given template name
	 */
	public Set<CustomNode> getAllocatedNodesByTemplateName(
			final String templateName) throws CloudProvisioningException {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to get servers list. \"" + templateName
					+ "\" is not a known template.");
		}

		allNodes.addAll(templateLists.get(NODES_LIST_ALLOCATED));

		return allNodes;
	}

	/**
	 * Retrieves all invalid nodes.
	 *
	 * @param templateName
	 *            The name of the nodes-list' template to use
	 * @return A collection of all the invalid nodes of the specified template
	 * @throws CloudProvisioningException
	 *             Indicates the servers list could not be obtained for the
	 *             given template name
	 */
	public Set<CustomNode> getInvalidNodesByTemplateName(
			final String templateName) throws CloudProvisioningException {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			throw new CloudProvisioningException(
					"Failed to get servers list. \"" + templateName
					+ "\" is not a known template.");
		}

		allNodes.addAll(templateLists.get(NODES_LIST_INVALID));

		return allNodes;
	}

	/**
	 * * Invalidates the given node (i.e. moves it from the free pool to the
	 * invalidated pool), so it will not be allocated unless all the free nodes
	 * are in use.
	 *
	 * @param templateName
	 *            The template this server belongs to
	 * @param serverName
	 *            The name of the server to invalidate
	 * @throws CloudProvisioningException
	 *             Indicates the server could not be marked as Invalid for the
	 *             specified template
	 */
	public synchronized void invalidateServer(final String templateName,
			final CustomNode serverName) throws CloudProvisioningException {
		// attempting to remove the invalid node from the active lists so it
		// will not be used anymore, just to
		// be sure.
		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
				.get(templateName);
		if (templateLists == null || templateLists.isEmpty()) {
			// todo illegal argument exception
			throw new CloudProvisioningException(
					"Failed to invalidate server. \"" + templateName
					+ "\" is not a known template.");
		}
		final List<CustomNode> freeNodesPool = templateLists
				.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists
				.get(NODES_LIST_ALLOCATED);
		final List<CustomNode> invalidNodesPool = templateLists
				.get(NODES_LIST_INVALID);

		freeNodesPool.remove(serverName);
		allocatedNodesPool.remove(serverName);
		if (!invalidNodesPool.contains(serverName)) {
			invalidNodesPool.add(serverName);
		}
	}

	/**
	 * closes the deployer, currently not doing anything.
	 */
	public void close() {
		// Do nothing
	}

	private List<CustomNode> getAllNodes() throws CloudProvisioningException {
		final List<CustomNode> allNodes = new ArrayList<CustomNode>();

		for (final String templateName : nodesListsByTemplates.keySet()) {
			final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates
					.get(templateName);
			if (templateLists == null || templateLists.isEmpty()) {
				throw new CloudProvisioningException(
						"Failed to get servers list. \"" + templateName
						+ "\" is not a known template.");
			}
			final List<CustomNode> freeNodesPool = templateLists
					.get(NODES_LIST_FREE);
			final List<CustomNode> allocatedNodesPool = templateLists
					.get(NODES_LIST_ALLOCATED);
			final List<CustomNode> invalidNodesPool = templateLists
					.get(NODES_LIST_INVALID);

			allNodes.addAll(freeNodesPool);
			allNodes.addAll(allocatedNodesPool);
			allNodes.addAll(invalidNodesPool);
		}

		return allNodes;
	}

	private List<CustomNode> parseCloudNodes(
			final List<Map<String, String>> nodesMapList)
					throws CloudProvisioningException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		for (final Map<String, String> nodeMap : nodesMapList) {

			String nodeId = nodeMap.get(CLOUD_NODE_ID);
			String hostList = nodeMap.get(CLOUD_NODE_HOST_LIST);
			String hostRange = nodeMap.get(CLOUD_NODE_HOST_RANGE);

			if (StringUtils.isBlank(nodeId)) {
				throw new CloudProvisioningException(EMPTY_HOSTS_ERR_MESSAGE);
			}

			if (StringUtils.isNotBlank(hostList)) {
				if (isIPList(hostList.trim())) {
					cloudNodes.addAll(parseNodeList(nodeMap));
				} else {
					cloudNodes.add(parseOneNode(nodeMap));
				}
			} else if (StringUtils.isNotBlank(hostRange)) {
				if (isIPRange(hostRange.trim())) {
					cloudNodes.addAll(parseNodeRange(nodeMap));
				} else if (isIPCIDR(hostRange.trim())) {
					cloudNodes.addAll(parseNodeCIDR(nodeMap));
				} else {
					throw new CloudProvisioningException(
							INVALID_HOSTS_ERR_MESSAGE + ": " + hostList);
				}
			} else {
				throw new CloudProvisioningException(INVALID_HOSTS_ERR_MESSAGE);
			}
		}

		return cloudNodes;
	}

	private boolean isIPList(final String hostList) {
		boolean result = false;

		if (hostList != null && hostList.contains(",")) {
			result = true;
		}

		return result;
	}

	private boolean isIPRange(final String hostRange)
			throws CloudProvisioningException {
		boolean result = false;

		if (hostRange.contains("-")) {
			final String ipRangeStart = hostRange.substring(0,
					hostRange.indexOf('-'));
			if (IPUtils.validateIPAddress(ipRangeStart)) {
				result = true;
			}
		}

		return result;
	}

	private boolean isIPCIDR(final String hostRange) {
		boolean result = false;

		if (hostRange.contains("/")) {
			result = true;
		}

		return result;
	}

	/**
	 * Checks if the specified id is a single-valued id or a template for
	 * multiple id-s.
	 *
	 * @param nodeId
	 *            The id to examine
	 * @return true if this id is a template, false if it's a single-values id.
	 */
	private boolean isIdTemplate(final String nodeId) {
		boolean result = false;

		if (nodeId.contains("{0}")) {
			result = true;
		}

		return result;
	}

	/**
	 * Parses a single byon cloud node.
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IP or host
	 *            name)
	 * @return A {@link CustomNode} object
	 */
	private CustomNode parseOneNode(final Map<String, String> nodeMap) {

		// It was decided not to validate the connection to the host at this
		// stage. commected out.

		/*
		 * String host = nodeMap.get(CLOUD_NODE_HOST_LIST); // validate the IP
		 * if (!IPUtils.validateIPAddress(host)) { // maybe this is a host name
		 * try { host = IPUtils.resolveHostName(host); } catch (final Exception
		 * e) { throw new
		 * CloudProvisioningException("Invalid host name or address: " + host);
		 * }
		 *
		 * if (!IPUtils.validateIPAddress(host)) { throw new
		 * CloudProvisioningException("Invalid host name or address: " + host);
		 * } }
		 */

		// create a new node
		return new CustomNodeImpl(PROVIDER_ID, nodeMap.get(CLOUD_NODE_ID)
				.trim(), nodeMap.get(CLOUD_NODE_HOST_LIST).trim(),
				nodeMap.get(CLOUD_NODE_USERNAME),
				nodeMap.get(CLOUD_NODE_CREDENTIAL), nodeMap.get(CLOUD_NODE_ID)
				.trim());
	}

	/**
	 * Parses a range of nodes (e.g. 192.168.9.1-192.168.9.8)
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IP range)
	 * @return A list of {@link CustomNode} objects
	 * @throws CloudProvisioningException
	 *             Indicated an invalid IP address is used
	 */
	private List<CustomNode> parseNodeRange(final Map<String, String> nodeMap)
			throws CloudProvisioningException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		boolean useIdAsTemplate = false;
		boolean useIdAsPrefix = false;
		int index = 1;
		String currnentId;

		final String nodeId = nodeMap.get(CLOUD_NODE_ID).trim();
		final String ipRange = nodeMap.get(CLOUD_NODE_HOST_RANGE).trim();

		// syntax validation (IPs are validated later, through IPUtils)
		final int ipDashIndex = ipRange.indexOf('-');
		if (ipDashIndex < 0) {
			throw new CloudProvisioningException(EMPTY_IP_RANGE_ERR_MESSAGE);
		}

		// run through the range of IPs
		final String ipRangeStart = ipRange.substring(0, ipRange.indexOf('-'));
		final String ipRangeEnd = ipRange.substring(ipRange.indexOf('-') + 1);

		if (IPUtils.ip2Long(ipRangeStart) < IPUtils.ip2Long(ipRangeEnd)) {
			if (isIdTemplate(nodeId)) {
				useIdAsTemplate = true;
			} else {
				useIdAsPrefix = true;
			}
		}

		String ip = ipRangeStart;
		while (IPUtils.ip2Long(ip) <= IPUtils.ip2Long(ipRangeEnd)) {

			// validate the IP
			ip = ip.trim();
			if (!IPUtils.validateIPAddress(ip)) {
				throw new CloudProvisioningException("Invalid IP address: "
						+ ip);
			}

			// set the id
			if (useIdAsTemplate) {
				currnentId = MessageFormat.format(nodeId, index);
			} else if (useIdAsPrefix) {
				currnentId = nodeId + index;
			} else {
				// just one IPs
				currnentId = nodeId;
			}

			// create a new node
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, currnentId, ip,
					nodeMap.get(CLOUD_NODE_USERNAME), nodeMap
					.get(CLOUD_NODE_CREDENTIAL), currnentId));

			index++;
			ip = IPUtils.getNextIP(ip);
		}

		return cloudNodes;
	}

	/**
	 * Parses a range of nodes formatted as a CIDR (e.g. 192.168.9.60/31)
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IPs as CIDR)
	 * @return A list of {@link CustomNode} objects
	 * @throws CloudProvisioningException
	 *             Indicated an invalid IP address is used
	 */
	private List<CustomNode> parseNodeCIDR(final Map<String, String> nodeMap)
			throws CloudProvisioningException {
		try {
			nodeMap.put(CLOUD_NODE_HOST_RANGE, IPUtils.ipCIDR2Range(nodeMap
					.get(CLOUD_NODE_HOST_RANGE).trim()));
		} catch (final Exception e) {
			throw new CloudProvisioningException(
					"Failed to start cloud machine.", e);
		}

		return parseNodeRange(nodeMap);
	}

	/**
	 * Parses a list of nodes (comma-separated IPs or host names).
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IPs or hosts
	 *            list)
	 * @return A list of {@link CustomNode} objects
	 */
	private List<CustomNode> parseNodeList(final Map<String, String> nodeMap) {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		boolean useIdAsTemplate = false;
		boolean useIdAsPrefix = false;
		int index = 1;
		String currnentId;

		final String nodeId = nodeMap.get(CLOUD_NODE_ID).trim();
		final String ipList = nodeMap.get(CLOUD_NODE_HOST_LIST).trim();

		final String[] ipsArr = ipList.split(",");
		if (ipsArr.length > 1) {
			if (isIdTemplate(nodeId)) {
				useIdAsTemplate = true;
			} else {
				useIdAsPrefix = true;
			}
		}

		for (String ip : ipsArr) {

			// validate the IP
			ip = ip.trim();

			/*
			 * if (!IPUtils.validateIPAddress(ip)) { // maybe this is a host
			 * name try { ip = IPUtils.resolveHostName(ip); } catch (final
			 * Exception e) { throw new
			 * CloudProvisioningException("Invalid or unreachable IP address: "
			 * + ip); }
			 *
			 * if (!IPUtils.validateIPAddress(ip)) { throw new
			 * CloudProvisioningException("Invalid or unreachable IP address: "
			 * + ip); } }
			 */

			// set the id
			if (useIdAsTemplate) {
				currnentId = MessageFormat.format(nodeId, index);
			} else if (useIdAsPrefix) {
				currnentId = nodeId + index;
			} else {
				// just one IPs
				currnentId = nodeId;
			}

			// create a new node
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, currnentId, ip,
					nodeMap.get(CLOUD_NODE_USERNAME), nodeMap
					.get(CLOUD_NODE_CREDENTIAL), currnentId));

			index++;
		}

		return cloudNodes;
	}

	/**
	 * Sets the initial nodes pools (free nodes, allocated, invalid and
	 * terminated) for each template. The initial allocated-nodes pool is always
	 * empty. The terminated nodes pool is currently not supported and remains
	 * empty all along.
	 *
	 * @param templateName
	 *            The name of the template
	 * @param resolvedNodes
	 *            The resolved nodes (will be set as free nodes ready for use)
	 * @param unresolvedNodes
	 *            The unresolved nodes (will be set as invalid as they aren't
	 *            reachable now)
	 */
	private void setInitialPoolsForTemplate(final String templateName,
			final List<CustomNode> resolvedNodes,
			final List<CustomNode> unresolvedNodes) {
		// todo use hashmap instead
		// use pojo instead of a complex map
		final Map<String, List<CustomNode>> templateLists = new Hashtable<String, List<CustomNode>>();
		final List<CustomNode> freeNodesPool = new ArrayList<CustomNode>();
		final List<CustomNode> invalidNodesPool = new ArrayList<CustomNode>();

		freeNodesPool.addAll(aggregateWithoutDuplicates(freeNodesPool,
				resolvedNodes));
		invalidNodesPool.addAll(aggregateWithoutDuplicates(invalidNodesPool,
				unresolvedNodes));

		templateLists.put(NODES_LIST_FREE, freeNodesPool);
		templateLists.put(NODES_LIST_ALLOCATED, new ArrayList<CustomNode>());
		templateLists.put(NODES_LIST_INVALID, invalidNodesPool);
		templateLists.put(NODES_LIST_TERMINATED, new ArrayList<CustomNode>());

		nodesListsByTemplates.put(templateName, templateLists);
	}

	private static List<CustomNode> removeDuplicates(
			final List<CustomNode> customNodesList) {
		final List<CustomNode> totalList = new ArrayList<CustomNode>();
		for (final CustomNode node : customNodesList) {
			if (!totalList.contains(node)) {
				totalList.add(node);
			}
		}
		return totalList;
	}

	private static Set<String> getDuplicateIPs(final List<CustomNode> oldNodes,
			final List<CustomNode> newNodes) {
		final Set<String> existingIPs = new HashSet<String>();

		for (final CustomNode newNode : newNodes) {
			for (final CustomNode oldNode : oldNodes) {
				if (oldNode.getResolvedIP().equalsIgnoreCase(
						newNode.getResolvedIP())) {
					existingIPs.add(oldNode.getResolvedIP());
					break;
				}
			}
		}

		return existingIPs;
	}

	private static List<CustomNode> aggregateWithoutDuplicates(
			final List<CustomNode> basicList, final List<CustomNode> newItems) {
		final List<CustomNode> totalList = new ArrayList<CustomNode>(basicList);
		for (final CustomNode node : newItems) {
			if (!totalList.contains(node)) {
				totalList.add(node);
			}
		}
		return totalList;
	}

	public List<String> getTemplatesList() {
		List<String> templatesList = new LinkedList<String>();
		templatesList.addAll(nodesListsByTemplates.keySet());
		return templatesList;
	}

	public void removeTemplates(List<String> redundantTemplates) throws CloudProvisioningException {
		for (String templateName : redundantTemplates) {
			Map<String, List<CustomNode>> nodesMap = nodesListsByTemplates.get(templateName);
			List<CustomNode> allocatedNodesList = nodesMap.get(NODES_LIST_ALLOCATED);
			if(allocatedNodesList != null && !allocatedNodesList.isEmpty()) {
				String errMsg = "Failed to remove template [" + templateName
						+ "] from deployer, some nodes are still allocated: " + allocatedNodesList;
				logger.log(Level.WARNING, errMsg);
				throw new CloudProvisioningException(errMsg);
			}
			nodesListsByTemplates.remove(templateName);
		}
	}
}
