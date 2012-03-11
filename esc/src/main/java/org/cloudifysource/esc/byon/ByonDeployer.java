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

		((CustomNodeImpl)node).setNodeName(name);

		return node;
	}
	
	public synchronized void setAllocated(final Set<String> IpAddresses) {
		for (String ipAddress : IpAddresses) {
			for (final CustomNode node : freeNodesPool) {
				if ((StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)) ||
						(StringUtils.isNotBlank(node.getPublicIP()) && node.getPublicIP().equalsIgnoreCase(ipAddress))) {
					freeNodesPool.remove(node);
					allocatedNodesPool.add(node);
					break;
				}
			}
		}
	}

	public synchronized void shutdownServer(final CustomNode node)  {
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
			if ((StringUtils.isNotBlank(node.getPrivateIP()) && node.getPrivateIP().equalsIgnoreCase(ipAddress)) ||
					(StringUtils.isNotBlank(node.getPublicIP()) && node.getPublicIP().equalsIgnoreCase(ipAddress))) {
				selectedNode = node;
				break;
			}
		}

		return selectedNode;
	}

	public synchronized void invalidateServer(final CustomNode node) {
		// attempting to remove the invalid node from both lists so it will not be used anymore, just to be sure.
		freeNodesPool.remove(node);
		allocatedNodesPool.remove(node);
		invalidNodesPool.add(node);
	}

	public void close() {
		// Do nothing
	}

	private Set<CustomNode> parseCloudNodes(final List<Map<String, String>> nodesMapList) throws Exception {

		final Set<CustomNode> cloudNodes = new HashSet<CustomNode>();

		for (Map<String, String> nodeMap : nodesMapList) {
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, nodeMap.get(CLOUD_NODE_ID), nodeMap
					.get(CLOUD_NODE_IP), nodeMap.get(CLOUD_NODE_USERNAME), nodeMap.get(CLOUD_NODE_CREDENTIAL),
					nodeMap.get(CLOUD_NODE_ID)));
		}

		return cloudNodes;
	}

}
