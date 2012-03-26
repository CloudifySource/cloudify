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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.byon.CustomNodeImpl;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.IPUtils;

/**
 * @author noak
 * @since 2.0.1
 * 
 *        Implements a cloud-simulator, using private machines as a pool of nodes on which the application is
 *        deployed. The list of available nodes and matching credentials are configured through the cloud
 *        Groovy file.
 */
public class ByonDeployer {

	private static final String PROVIDER_ID = "BYON";
	private static final String NODES_LIST_FREE = "FREE";
	private static final String NODES_LIST_ALLOCATED = "ALLOCATED";
	private static final String NODES_LIST_INVALID = "INVALID";
	private static final String CLOUD_NODE_ID = "id";
	private static final String CLOUD_NODE_IP = "ip";
	private static final String CLOUD_NODE_ID_PREFIX = "idPrefix";
	private static final String CLOUD_NODE_IP_RANGE = "ipRange";
	private static final String CLOUD_NODE_IP_LIST = "ipList";
	private static final String CLOUD_NODE_IP_CIDR = "CIDR";
	private static final String CLOUD_NODE_USERNAME = "username";
	private static final String CLOUD_NODE_CREDENTIAL = "credential";

	private final Map<String, Map<String, List<CustomNode>>> nodesListsByTemplates = new Hashtable<String, Map<String, List<CustomNode>>>();

	/**
	 * Constructor.
	 */
	public ByonDeployer() throws Exception {
	}

	/**
	 * Adds a list of nodes related to a specific template.
	 * 
	 * @param templateName
	 *            The name of the template this nodes-list belongs to
	 * @param managementNodesList
	 *            A list of maps, each map representing a cloud node
	 * @throws Exception
	 *             Indicates the node parsing failed
	 */
	public synchronized void addNodesList(final String templateName, final List<Map<String, String>> nodesList)
			throws Exception {
		final List<CustomNode> parsedNodes = parseCloudNodes(nodesList);
		final Map<String, List<CustomNode>> templateLists = new Hashtable<String, List<CustomNode>>();
		final List<CustomNode> freeNodesPool = new ArrayList<CustomNode>();
		freeNodesPool.addAll(aggregate(freeNodesPool, parsedNodes));
		templateLists.put(NODES_LIST_FREE, freeNodesPool);
		templateLists.put(NODES_LIST_ALLOCATED, new ArrayList<CustomNode>());
		templateLists.put(NODES_LIST_INVALID, new ArrayList<CustomNode>());

		nodesListsByTemplates.put(templateName, templateLists);
	}

	/**
	 * Creates a server (AKA a machine or a node) with the assigned logical name.
	 * 
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverName
	 *            A logical name used to uniquely identify this node (does not have to match the host name)
	 * @return A node available for use
	 * @throws InstallerException
	 *             Indicated a new machine could not be allocated, either because the name is empty or because
	 *             the nodes pool is exhausted
	 */
	public synchronized CustomNode createServer(final String templateName, final String serverName)
			throws InstallerException {
		CustomNode node = null;

		if (org.apache.commons.lang.StringUtils.isBlank(serverName)) {
			throw new InstallerException("Failed to start cloud node, server name is missing");
		}

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates.get(templateName);
		final List<CustomNode> freeNodesPool = templateLists.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists.get(NODES_LIST_ALLOCATED);

		if (freeNodesPool.size() == 0) {
			throw new InstallerException("Failed to create new cloud node, all nodes are currently used");
		}

		node = freeNodesPool.iterator().next();

		if (!allocatedNodesPool.contains(node)) {
			allocatedNodesPool.add(node);
		}
		freeNodesPool.remove(node);
		((CustomNodeImpl) node).setNodeName(serverName);

		return node;
	}

	/**
	 * Sets the nodes holding certain IPs as allocated, so they would not be re-allocated to other clients.
	 * 
	 * @param templateName
	 *            The name of the nodes-list' template the IPs belongs to
	 * @param ipAddresses
	 *            A set of IP addresses (decimal dotted format)
	 */
	public synchronized void setAllocated(final String templateName, final Set<String> ipAddresses) {
		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates.get(templateName);
		final List<CustomNode> freeNodesPool = templateLists.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists.get(NODES_LIST_ALLOCATED);

		for (final String ipAddress : ipAddresses) {
			for (final CustomNode node : freeNodesPool) {
				if (StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)
						|| StringUtils.isNotBlank(node.getPublicIP())
						&& node.getPublicIP().equalsIgnoreCase(ipAddress)) {
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
	 * Shuts down a given node, (moves the node back to the free nodes list, to be used again).
	 * 
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverName
	 *            A server to shutdown
	 */
	public synchronized void shutdownServer(final String templateName, final CustomNode serverName) {
		if (serverName == null) {
			return;
		}

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates.get(templateName);
		final List<CustomNode> freeNodesPool = templateLists.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists.get(NODES_LIST_ALLOCATED);

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
	 */
	public void shutdownServerById(final String templateName, final String serverId) {
		shutdownServer(templateName, getServerByID(templateName, serverId));
	}

	/**
	 * Shuts down the server with the given IP address.
	 * 
	 * @param templateName
	 *            The name of the nodes-list' template this server belongs to
	 * @param serverIp
	 *            The IP of the server to shutdown (dotted decimal format)
	 */
	public void shutdownServerByIp(final String templateName, final String serverIp) {
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
	 */
	public CustomNode getServerByName(final String templateName, final String serverName) {
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
	 */
	public CustomNode getServerByID(final String templateName, final String id) {
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
	 */
	public CustomNode getServerByIP(final String templateName, final String ipAddress) {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodesByTemplateName(templateName)) {
			if (StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)
					|| StringUtils.isNotBlank(node.getPublicIP()) && node.getPublicIP().equalsIgnoreCase(ipAddress)) {
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
	 */
	public Set<CustomNode> getAllNodesByTemplateName(final String templateName) {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates.get(templateName);
		final List<CustomNode> freeNodesPool = templateLists.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists.get(NODES_LIST_ALLOCATED);
		final List<CustomNode> invalidNodesPool = templateLists.get(NODES_LIST_INVALID);

		allNodes.addAll(freeNodesPool);
		allNodes.addAll(allocatedNodesPool);
		allNodes.addAll(invalidNodesPool);

		return allNodes;
	}

	/**
	 * Invalidates the given node (i.e. moves it from the free pool to the invalidated pool), so it will not
	 * be allocated unless all the free nodes are in use.
	 * 
	 * @param node
	 *            The node to invalidate
	 */
	public synchronized void invalidateServer(final String templateName, final CustomNode serverName) {
		// attempting to remove the invalid node from the active lists so it will not be used anymore, just to
		// be
		// sure.
		final Map<String, List<CustomNode>> templateLists = nodesListsByTemplates.get(templateName);
		final List<CustomNode> freeNodesPool = templateLists.get(NODES_LIST_FREE);
		final List<CustomNode> allocatedNodesPool = templateLists.get(NODES_LIST_ALLOCATED);
		final List<CustomNode> invalidNodesPool = templateLists.get(NODES_LIST_INVALID);

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

	private List<CustomNode> parseCloudNodes(final List<Map<String, String>> nodesMapList) throws Exception {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		for (final Map<String, String> nodeMap : nodesMapList) {
			if (StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_ID))
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP))) {
				cloudNodes.add(parseOneNode(nodeMap));
			} else if (StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_ID_PREFIX))
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP_LIST))) {
				cloudNodes.addAll(parseNodeList(nodeMap));
			} else if (StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_ID_PREFIX))
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP_CIDR))) {
				cloudNodes.addAll(parseNodeCIDR(nodeMap));
			} else if (StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_ID_PREFIX))
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP_RANGE))) {
				cloudNodes.addAll(parseNodeRange(nodeMap));
			} else {
				throw new InstallerException("Failed to start cloud node, invalid IP/ID configuration.");
			}
		}

		return cloudNodes;
	}

	private CustomNode parseOneNode(final Map<String, String> nodeMap) throws InstallerException {
		final String ipAddress = nodeMap.get(CLOUD_NODE_IP);
		if (!IPUtils.validateIPAddress(ipAddress)) {
			throw new InstallerException("Invalid IP address: " + ipAddress);
		}

		return new CustomNodeImpl(PROVIDER_ID, nodeMap.get(CLOUD_NODE_ID), ipAddress,
				nodeMap.get(CLOUD_NODE_USERNAME), nodeMap.get(CLOUD_NODE_CREDENTIAL), nodeMap.get(CLOUD_NODE_ID));
	}

	private List<CustomNode> parseNodeRange(final Map<String, String> nodeMap) throws InstallerException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();
		final String idPrefix = nodeMap.get(CLOUD_NODE_ID_PREFIX);
		final String ipRange = nodeMap.get(CLOUD_NODE_IP_RANGE);

		// syntax validation (IPs are validated later, through IPUtils)
		final int ipDashIndex = ipRange.indexOf("-");
		if (ipDashIndex < 0) {
			throw new InstallerException("Failed to start cloud node, invalid IP range configuration: " + ipRange
					+ " is missing the token \"-\"");
		}

		// run through the range of IPs
		final String ipRangeStart = ipRange.substring(0, ipRange.indexOf("-"));
		final String ipRangeEnd = ipRange.substring(ipRange.indexOf("-") + 1);

		String ip = ipRangeStart;
		int index = 1;
		try {
			while (IPUtils.ip2Long(ip) <= IPUtils.ip2Long(ipRangeEnd)) {
				cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, idPrefix + index, ip, nodeMap.get(CLOUD_NODE_USERNAME),
						nodeMap.get(CLOUD_NODE_CREDENTIAL), idPrefix + index));
				index++;
				ip = IPUtils.getNextIP(ip);
			}
		} catch (final Exception e) {
			throw new InstallerException("Failed to start cloud machine.", e);
		}

		return cloudNodes;
	}

	private List<CustomNode> parseNodeCIDR(final Map<String, String> nodeMap) throws InstallerException {
		final String ipCIDR = nodeMap.get(CLOUD_NODE_IP_CIDR);
		try {
			nodeMap.put(CLOUD_NODE_IP_RANGE, IPUtils.ipCIDR2Range(ipCIDR));
		} catch (final Exception e) {
			throw new InstallerException("Failed to start cloud machine.", e);
		}

		return parseNodeRange(nodeMap);
	}

	private List<CustomNode> parseNodeList(final Map<String, String> nodeMap) throws InstallerException {
		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();
		final String idPrefix = nodeMap.get(CLOUD_NODE_ID_PREFIX);
		final String ipList = nodeMap.get(CLOUD_NODE_IP_LIST);
		final String[] ipsArr = ipList.split(",");
		int index = 1;
		for (String ip : ipsArr) {
			ip = ip.trim();
			if (!IPUtils.validateIPAddress(ip)) {
				throw new InstallerException("Invalid IP address: " + ip);
			}
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, idPrefix + index, ip, nodeMap.get(CLOUD_NODE_USERNAME),
					nodeMap.get(CLOUD_NODE_CREDENTIAL), idPrefix + index));
			index++;
		}

		return cloudNodes;
	}

	private static List<CustomNode> aggregate(final List<CustomNode> basicList, final List<CustomNode> newItems) {
		final List<CustomNode> totalList = new ArrayList<CustomNode>(basicList);
		for (final CustomNode node : newItems) {
			if (!totalList.contains(node)) {
				totalList.add(node);
			}
		}
		return totalList;
	}
}
