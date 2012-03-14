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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.byon.CustomNodeImpl;
import org.cloudifysource.esc.installer.InstallerException;

public class ByonDeployer {

	private static final String PROVIDER_ID = "BYON";
	private static final String CLOUD_NODE_ID = "id";
	private static final String CLOUD_NODE_IP = "ip";
	private static final String CLOUD_NODE_ID_PREFIX = "idPrefix";
	private static final String CLOUD_NODE_IP_RANGE = "ipRange";
	private static final String CLOUD_NODE_IP_CIDR = "CIDR";
	private static final String CLOUD_NODE_USERNAME = "username";
	private static final String CLOUD_NODE_CREDENTIAL = "credential";

	private final Set<CustomNode> freeNodesPool = new HashSet<CustomNode>();
	private final Set<CustomNode> allocatedNodesPool = new HashSet<CustomNode>();
	private final Set<CustomNode> invalidNodesPool = new HashSet<CustomNode>();

	public ByonDeployer(final List<Map<String, String>> nodesList) throws Exception {
		freeNodesPool.addAll(parseCloudNodes(nodesList));
	}

	public synchronized CustomNode createServer(final String name) throws InstallerException {

		CustomNode node = null;

		if (org.apache.commons.lang.StringUtils.isBlank(name)) {
			throw new InstallerException("Failed to start cloud node, server name is missing");
		}

		if (freeNodesPool.size() == 0) {
			throw new InstallerException("Failed to create new cloud node, all nodes are currently used");
		}

		node = freeNodesPool.iterator().next();

		freeNodesPool.remove(node);
		allocatedNodesPool.add(node);

		((CustomNodeImpl) node).setNodeName(name);

		return node;
	}

	public synchronized void setAllocated(final Set<String> IpAddresses) {
		for (final String ipAddress : IpAddresses) {
			for (final CustomNode node : freeNodesPool) {
				if (StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)
						|| StringUtils.isNotBlank(node.getPublicIP())
						&& node.getPublicIP().equalsIgnoreCase(ipAddress)) {
					freeNodesPool.remove(node);
					allocatedNodesPool.add(node);
					break;
				}
			}
		}
	}

	public synchronized void shutdownServer(final CustomNode node) {
		if (node == null) {
			return;
		}

		((CustomNodeImpl) node).setGroup(null);
		allocatedNodesPool.remove(node);
		freeNodesPool.add(node);
	}

	public void shutdownServerById(final String serverId) {
		shutdownServer(getServerByID(serverId));
	}

	public void shutdownServerByIp(final String serverIp) {
		shutdownServer(getServerByIP(serverIp));
	}

	public CustomNode getServerByName(final String serverName) {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodes()) {
			if (node.getNodeName().equalsIgnoreCase(serverName)) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	public Set<CustomNode> getAllNodes() {
		final Set<CustomNode> allNodes = new HashSet<CustomNode>();

		allNodes.addAll(freeNodesPool);
		allNodes.addAll(allocatedNodesPool);
		allNodes.addAll(invalidNodesPool);

		return allNodes;
	}

	public CustomNode getServerByID(final String id) {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodes()) {
			if (node.getId().equalsIgnoreCase(id)) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	public CustomNode getServerByIP(final String ipAddress) {
		CustomNode selectedNode = null;

		for (final CustomNode node : getAllNodes()) {
			if (StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)
					|| StringUtils.isNotBlank(node.getPublicIP()) && node.getPublicIP().equalsIgnoreCase(ipAddress)) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	public synchronized void invalidateServer(final CustomNode node) {
		// attempting to remove the invalid node from both lists so it will not be used anymore, just to be
		// sure.
		freeNodesPool.remove(node);
		allocatedNodesPool.remove(node);
		invalidNodesPool.add(node);
	}

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
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP_RANGE))) {
				cloudNodes.addAll(parseNodeRange(nodeMap));
			} else if (StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_ID_PREFIX))
					&& StringUtils.isNotBlank(nodeMap.get(CLOUD_NODE_IP_CIDR))) {
				cloudNodes.addAll(parseNodeCIDR(nodeMap));
			} else {
				throw new InstallerException("Failed to start cloud node, invalid IP/ID configuration.");
			}
		}

		return cloudNodes;
	}

	private CustomNode parseOneNode(final Map<String, String> nodeMap) {
		return new CustomNodeImpl(PROVIDER_ID, nodeMap.get(CLOUD_NODE_ID), nodeMap.get(CLOUD_NODE_IP),
				nodeMap.get(CLOUD_NODE_USERNAME), nodeMap.get(CLOUD_NODE_CREDENTIAL), nodeMap.get(CLOUD_NODE_ID));
	}

	private List<CustomNode> parseNodeRange(final Map<String, String> nodeMap) throws InstallerException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();
		final String idPrefix = nodeMap.get(CLOUD_NODE_ID_PREFIX);
		String ipRange = nodeMap.get(CLOUD_NODE_IP_RANGE);

		final int ipDashIndex = ipRange.indexOf("-");
		int ipLastDotIndex = StringUtils.lastIndexOf(ipRange, ".");
		int ipDotsCount = StringUtils.countMatches(ipRange, ".");
		if (ipLastDotIndex > ipDashIndex && ipDotsCount == 6) {
			ipRange = ipRange.substring(0, ipDashIndex + 1) + ipRange.substring(ipLastDotIndex + 1);
			ipLastDotIndex = StringUtils.lastIndexOf(ipRange, ".");
			ipDotsCount = StringUtils.countMatches(ipRange, ".");
		}

		// some validations
		if (ipDashIndex < 0) {
			throw new InstallerException("Failed to start cloud node, invalid " + CLOUD_NODE_IP_RANGE + "configuration: "
					+ ipRange + " is missing the token \"-\"");
		}

		if (ipDotsCount != 3 || ipDashIndex < ipLastDotIndex) {
			throw new InstallerException("Failed to start cloud node, invalid " + CLOUD_NODE_IP_RANGE + " configuration: "
					+ ipRange);
		}

		// run through the range of IPs
		final String ipPrefix = ipRange.substring(0, ipLastDotIndex + 1);
		final String ipSuffix = ipRange.substring(ipLastDotIndex + 1);
		final int ipRangeStart = Integer.parseInt(ipSuffix.substring(0, ipSuffix.indexOf("-")));
		final int ipRangeEnd = Integer.parseInt(ipSuffix.substring(ipSuffix.indexOf("-") + 1));

		for (int ip = ipRangeStart, index = 1; ip <= ipRangeEnd; ip++, index++) {
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, idPrefix + index, ipPrefix + ip, nodeMap
					.get(CLOUD_NODE_USERNAME), nodeMap.get(CLOUD_NODE_CREDENTIAL), idPrefix + index));
		}

		return cloudNodes;
	}

	private List<CustomNode> parseNodeCIDR(final Map<String, String> nodeMap) throws InstallerException {
		final String ipCIDR = nodeMap.get(CLOUD_NODE_IP_CIDR);
		try {
			nodeMap.put(CLOUD_NODE_IP_RANGE, ipCIDR2Range(ipCIDR));
		} catch (final Exception e) {
			throw new InstallerException("Failed to start cloud node, invalid " + CLOUD_NODE_IP_CIDR
					+ " configuration: " + ipCIDR);
		}

		return parseNodeRange(nodeMap);
	}

	private String ipCIDR2Range(final String ipCidr) throws UnknownHostException {

		final String[] parts = ipCidr.split("/");
		final String ip = parts[0];
		int maskBits;
		if (parts.length < 2) {
			maskBits = 0;
		} else {
			maskBits = Integer.parseInt(parts[1]);
		}

		// Step 1. Convert IPs into ints (32 bits).
		// E.g. 157.166.224.26 becomes 10011101 10100110 11100000 00011010
		final String[] ipParts = StringUtils.split(ip, ".");
		final int addr = Integer.parseInt(ipParts[0]) << 24 & 0xFF000000 | Integer.parseInt(ipParts[1]) << 16
				& 0xFF0000 | Integer.parseInt(ipParts[2]) << 8 & 0xFF00 | Integer.parseInt(ipParts[3]) & 0xFF;

		// Step 2. Get CIDR mask
		final int mask = 0xffffffff << 32 - maskBits;
		final int value = mask;
		final byte[] bytes = new byte[] { (byte) (value >>> 24), (byte) (value >> 16 & 0xff),
				(byte) (value >> 8 & 0xff), (byte) (value & 0xff) };

		final InetAddress netAddr = InetAddress.getByAddress(bytes);

		// Step 3. Find lowest IP address
		final int lowest = addr & mask;
		final String lowestIP = format(toArray(lowest));

		// Step 4. Find highest IP address
		final int highest = lowest + ~mask;
		final String highestIP = format(toArray(highest));

		return lowestIP + "-" + highestIP;
	}

	/*
	 * Convert a packed integer address into a 4-element array
	 */
	private int[] toArray(final int val) {
		final int ret[] = new int[4];
		for (int j = 3; j >= 0; --j) {
			ret[j] |= val >>> 8 * (3 - j) & 0xff;
		}
		return ret;
	}

	/*
	 * Convert a 4-element array into dotted decimal format
	 */
	private String format(final int[] octets) {
		final StringBuilder str = new StringBuilder();
		for (int i = 0; i < octets.length; ++i) {
			str.append(octets[i]);
			if (i != octets.length - 1) {
				str.append(".");
			}
		}
		return str.toString();
	}

}
