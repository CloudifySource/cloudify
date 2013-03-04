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
package org.cloudifysource.esc.driver.provisioning.byon;

import org.cloudifysource.esc.driver.provisioning.CustomNode;

/**
 * Implementation for a custom cloud node, used for example by the BYON cloud driver.
 *
 * @author noak
 * @since 2.0.1
 */
public class CustomNodeImpl implements CustomNode {

	/**
	 * The default port through which a node is connected is 22 (SSH).
	 */
	public static final int DEFAULT_LOGIN_PORT = 22;

	// TODO : Currently using private IP. Add public IP in phase 2.
	private final String providerId, id, ipAddress;

	private String resolvedIpAddress;

	private String nodeName;

	private String group;
	// credential can be a password or a private key
	private final String username, credential;

	private int loginPort = DEFAULT_LOGIN_PORT;

	/**
	 * Constructor.
	 *
	 * @param providerId
	 *            The cloud provider (e.g. EC2, BYON)
	 * @param id
	 *            A unique ID for the node
	 * @param ipAddress
	 *            The node's IP address or host name
	 * @param username
	 *            The username required to access the node
	 * @param credential
	 *            The password required to access the node
	 */
	public CustomNodeImpl(final String providerId, final String id, final String ipAddress, final String username,
			final String credential) {
		this(providerId, id, ipAddress, username, credential, ""/* nodeName */);
	}

	/**
	 * Constructor.
	 *
	 * @param providerId
	 *            The cloud provider (e.g. EC2, BYON)
	 * @param id
	 *            A unique ID for the node
	 * @param ipAddress
	 *            The node's IP address
	 * @param username
	 *            The username required to access the node
	 * @param credential
	 *            The password required to access the node
	 * @param nodeName
	 *            A unique logical name for the node, optional.
	 */
	public CustomNodeImpl(final String providerId, final String id, final String ipAddress, final String username,
			final String credential, final String nodeName) {
		this.providerId = providerId;
		this.id = id;
		this.ipAddress = ipAddress;
		this.username = username;
		this.credential = credential;
		this.nodeName = nodeName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setGroup(final String group) {
		this.group = group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProviderId() {
		return providerId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNodeName(final String nodeName) {
		this.nodeName = nodeName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getGroup() {
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCredential() {
		return credential;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPublicIP() {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPrivateIP() {
		return ipAddress;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setResolvedIP(final String resolvedIP) {
		this.resolvedIpAddress = resolvedIP;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getResolvedIP() {
		return resolvedIpAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLoginPort() {
		return this.loginPort;
	}

	@Override
	public void setLoginPort(final int loginPort) {
		this.loginPort = loginPort;
	}

	@Override
	public String toString() {
		return "[id=" + getId() + ", providerId=" + getProviderId() + ", group=" + getGroup() + ", nodeName="
				+ getNodeName() + ", loginPort=" + getLoginPort() + ", privateAddresses=" + getPrivateIP()
				+ ", publicAddresses=" + getPublicIP() + ", username=" + getUsername() + "]";
	}

	@Override
	public int hashCode() {
		// Ignore these constants - they are only used to calculate a hashcode.
		//CHECKSTYLE:OFF
		final int prime = 31;
		int result = 7;
		//CHECKSTYLE:ON
		result = prime * result + getLoginPort();
		result = prime * result + (getPrivateIP() == null ? 0 : getPrivateIP().hashCode());
		result = prime * result + (getPublicIP() == null ? 0 : getPublicIP().hashCode());
		result = prime * result + (providerId == null ? 0 : providerId.hashCode());
		result = prime * result + (nodeName == null ? 0 : nodeName.hashCode());
		result = prime * result + (group == null ? 0 : group.hashCode());
		return (prime * result + (username == null ? 0 : username.hashCode()));
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CustomNodeImpl other = (CustomNodeImpl) obj;
		if (getLoginPort() != other.getLoginPort()) {
			return false;
		}
		if (getPrivateIP() == null) {
			if (other.getPrivateIP() != null) {
				return false;
			}
		} else if (!getPrivateIP().equalsIgnoreCase(other.getPrivateIP())) {
			return false;
		}
		if (getPublicIP() == null) {
			if (other.getPublicIP() != null) {
				return false;
			}
		} else if (!getPublicIP().equalsIgnoreCase(other.getPublicIP())) {
			return false;
		}
		if (getProviderId() == null) {
			if (other.getProviderId() != null) {
				return false;
			}
		} else if (!getProviderId().equalsIgnoreCase(other.getProviderId())) {
			return false;
		}
		if (getNodeName() == null) {
			if (other.getNodeName() != null) {
				return false;
			}
		} else if (!getNodeName().equalsIgnoreCase(other.getNodeName())) {
			return false;
		}
		if (getGroup() == null) {
			if (other.getGroup() != null) {
				return false;
			}
		} else if (!getGroup().equalsIgnoreCase(other.getGroup())) {
			return false;
		}
		if (getUsername() == null) {
			if (other.getUsername() != null) {
				return false;
			}
		} else if (!getUsername().equalsIgnoreCase(other.getUsername())) {
			return false;
		}
		return true;
	}

}
