package org.cloudifysource.esc.byon;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.byon.CustomNodeImpl;

public class ByonUtils {
	
	private static final String PROVIDER_ID = "BYON";
	private static final String NODES_LIST = "nodesList";
	private static final String NODE_ID = "id";
	private static final String NODE_HOST_LIST = "host-list";
	private static final String NODE_HOST_RANGE = "host-range";
	private static final String NODE_USERNAME = "username";
	private static final String NODE_KEY_FILE = "keyFile";
	private static final String NODE_CREDENTIAL = "credential";
	private static final String EMPTY_ID_ERR_MESSAGE = "Failed to parse cloud nodes, empty ID configuration";
	private static final String EMPTY_HOSTS_ERR_MESSAGE = "Failed to parse cloud nodes, host list or range not set";
	private static final String INVALID_HOSTS_ERR_MESSAGE = "Failed to parse cloud nodes, invalid hosts configuration";
	private static final String EMPTY_IP_RANGE_ERR_MESSAGE = "Failed to parse cloud nodes, invalid IP range "
			+ "configuration: missing \"-\"";
	
	private static final Logger logger = Logger.getLogger(ByonUtils.class.getName());
	
	
	/**
	 * Parse the templates's nodes list and return a list of nodes, as Strings.
	 * @param template The template object to parse
	 * @return a list of nodes, as Strings, as expected by the parseCloudNodes.
	 * @throws CloudProvisioningException Indicates a failure to parse the given template.
	 */
	public static List<Map<String, String>> getTemplateNodesList(final ComputeTemplate template) 
			throws CloudProvisioningException {
		List<Map<Object, Object>> originalNodesList = null;
		final Map<String, Object> customSettings = template.getCustom();
		if (customSettings != null) {
			originalNodesList = (List<Map<Object, Object>>) customSettings.get(NODES_LIST);
		}
		
		if(originalNodesList == null || originalNodesList.isEmpty()) {
			logger.warning("Nodes list is missing. custom = " + customSettings);
			throw new CloudProvisioningException("Nodes list not set");
		}
		return ByonUtils.convertToStringMap(originalNodesList);
	}

	/**
	 * Parses the nodes defined in the given {@link ComputeTemplate} object.
	 * @param template The template to parse.
	 * @return a list of {@link CustomNode} objects.
	 * @throws CloudProvisioningException Indicates a failure to parse the given template.
	 */
	public static List<CustomNode> parseCloudNodes(final ComputeTemplate template) throws CloudProvisioningException {
		List<Map<String, String>> nodesList = ByonUtils.getTemplateNodesList(template);
		return parseCloudNodes(nodesList);
	}
	
	
	/**
	 * Parses the nodes defined in the given list to create a list of {@link CustomNode} objects.
	 * @param nodesMapList The list of nodes to parse, as specified in the cloud configuration file.
	 * @return a list of {@link CustomNode} objects.
	 * @throws CloudProvisioningException Indicates a failure to parse the given nodes list.
	 */
	public static List<CustomNode> parseCloudNodes(
			final List<Map<String, String>> nodesMapList)
			throws CloudProvisioningException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		for (final Map<String, String> nodeMap : nodesMapList) {

			String nodeId = nodeMap.get(NODE_ID);
			String hostList = nodeMap.get(NODE_HOST_LIST);
			String hostRange = nodeMap.get(NODE_HOST_RANGE);

			if (StringUtils.isBlank(nodeId)) {
				throw new CloudProvisioningException(EMPTY_ID_ERR_MESSAGE);
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
							INVALID_HOSTS_ERR_MESSAGE + ": " + hostRange);
				}
			} else {
				//host list or range not set 
				throw new CloudProvisioningException(EMPTY_HOSTS_ERR_MESSAGE);
			}
		}

		return cloudNodes;
	}
	

	/**
	 * Parses a single byon cloud node.
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IP or host name)
	 * @return A {@link CustomNode} object
	 */
	public static CustomNode parseOneNode(final Map<String, String> nodeMap) {

		// Handle the edge case scenario where a host-list defines a single node
		String nodeId = nodeMap.get(NODE_ID);
		if (nodeId.contains("{")) {
			nodeId = MessageFormat.format(nodeId, 1);
		}

        String host = nodeMap.get(NODE_HOST_LIST).trim();
        String ip = null;
        String hostName = null;
        if (IPUtils.validateIPAddress(host)) {
            // the host is an ip address
            ip = host;
        } else {
            hostName = host;
        }

        return new CustomNodeImpl(PROVIDER_ID, nodeId.trim(),
                ip, hostName, nodeMap.get(NODE_USERNAME),
				nodeMap.get(NODE_CREDENTIAL), nodeMap.get(NODE_KEY_FILE), nodeId.trim());
	}
	
	
	/**
	 * Parses a list of nodes (comma-separated IPs or host names).
	 *
	 * @param nodeMap
	 *            The map of attributes related to this node (ID, IPs or hosts list)
	 * @return A list of {@link CustomNode} objects
	 */
	public static List<CustomNode> parseNodeList(final Map<String, String> nodeMap) {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		boolean useIdAsTemplate = false;
		boolean useIdAsPrefix = false;
		int index = 1;
		String currnentId;

		final String nodeId = nodeMap.get(NODE_ID).trim();
		final String hostsList = nodeMap.get(NODE_HOST_LIST).trim();

		final String[] hosts = hostsList.split(",");
		if (hosts.length > 1) {
			if (isIdTemplate(nodeId)) {
				useIdAsTemplate = true;
			} else {
				useIdAsPrefix = true;
			}
		}

		for (String host : hosts) {

			// validate the IP
			host = host.trim();

			if (host.isEmpty()) {
				continue;
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

            String ip = null;
            String hostName = null;
            if (IPUtils.validateIPAddress(host)) {
                // the host is an ip address
                ip = host;
            } else {
                hostName = host;
            }

			// create a new node
			cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, currnentId, ip, hostName,
					nodeMap.get(NODE_USERNAME), nodeMap.get(NODE_CREDENTIAL),
					nodeMap.get(NODE_KEY_FILE), currnentId));

			index++;
		}

		return cloudNodes;
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
	public static List<CustomNode> parseNodeRange(final Map<String, String> nodeMap)
			throws CloudProvisioningException {

		final List<CustomNode> cloudNodes = new ArrayList<CustomNode>();

		boolean useIdAsTemplate = false;
		boolean useIdAsPrefix = false;
		int index = 1;
		String currnentId;

		final String nodeId = nodeMap.get(NODE_ID).trim();
		final String ipRange = nodeMap.get(NODE_HOST_RANGE).trim();

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

            cloudNodes.add(new CustomNodeImpl(PROVIDER_ID, currnentId, ip, null,
					nodeMap.get(NODE_USERNAME), nodeMap.get(NODE_CREDENTIAL),
					nodeMap.get(NODE_KEY_FILE), currnentId));

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
	public static List<CustomNode> parseNodeCIDR(final Map<String, String> nodeMap)
			throws CloudProvisioningException {
		try {
			nodeMap.put(NODE_HOST_RANGE, IPUtils.ipCIDR2Range(nodeMap
					.get(NODE_HOST_RANGE).trim()));
		} catch (final Exception e) {
			throw new CloudProvisioningException(
					"Failed to start cloud machine.", e);
		}

		return parseNodeRange(nodeMap);
	}
	
	
	/*******
	 * It is easy to accidentally create GStrings instead of String in a groovy file. This will auto correct the problem
	 * for byon node definitions by calling the toString() methods for map keys and values.
	 *
	 * @param originalNodesList
	 *            .
	 * @return the
	 */
	public static List<Map<String, String>> convertToStringMap(final List<Map<Object, Object>> originalNodesList) {
		List<Map<String, String>> nodesList;
		nodesList = new LinkedList<Map<String, String>>();

		for (final Map<Object, Object> originalMap : originalNodesList) {
			final Map<String, String> newMap = new LinkedHashMap<String, String>();
			final Set<Entry<Object, Object>> entries = originalMap.entrySet();
			for (final Entry<Object, Object> entry : entries) {
				newMap.put(entry.getKey().toString(), entry.getValue().toString());
			}
			nodesList.add(newMap);

		}
		return nodesList;
	}
	
	
	/**
	 * Checks if the specified id is a single-valued id or a template for multiple id-s.
	 *
	 * @param nodeId
	 *            The id to examine
	 * @return true if this id is a template, false if it's a single-values id.
	 */
	private static boolean isIdTemplate(final String nodeId) {
		boolean result = false;

		if (nodeId.contains("{0}")) {
			result = true;
		}

		return result;
	}
	

	private static boolean isIPList(final String hostList) {
		boolean result = false;

		if (hostList != null && hostList.contains(",")) {
			result = true;
		}

		return result;
	}
	

	private static boolean isIPRange(final String hostRange)
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
	

	private static boolean isIPCIDR(final String hostRange) {
		boolean result = false;

		if (hostRange.contains("/")) {
			result = true;
		}

		return result;
	}

}
