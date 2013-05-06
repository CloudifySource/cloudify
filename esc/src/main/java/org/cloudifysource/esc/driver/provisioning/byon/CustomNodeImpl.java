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

import org.apache.commons.lang.StringUtils;
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

	private String providerId;
	private String id;

    // TODO : Support public IP
	private String privateIp;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    private String hostName;
	private String nodeName;
	private String group;
	private String username;
	private String credential;	// password
	private String keyFile;		// private key file

	private int loginPort = DEFAULT_LOGIN_PORT;

	/**
	 * Constructor.
	 *
	 * @param providerId
	 *            The cloud provider (e.g. EC2, BYON)
	 * @param id
	 *            A unique ID for the node
	 * @param privateIp
	 *            The node's IP address or host name
	 * @param username
	 *            The username required to access the node
	 * @param credential
	 *            The password required to access the node (optional)
	 * @param keyFile
	 *            The private key file required to access the node  (optional)
	 */
	public CustomNodeImpl(final String providerId, final String id, final String privateIp,
                          final String hostName,
                          final String username,
			final String credential, final String keyFile) {
		this(providerId, id, privateIp, hostName, username, credential, keyFile, ""/* nodeName */);
	}

	/**
	 * Constructor.
	 *
	 * @param providerId
	 *            The cloud provider (e.g. EC2, BYON)
	 * @param id
	 *            A unique ID for the node
	 * @param privateIp
	 *            The node's IP address
	 * @param username
	 *            The username required to access the node
	 * @param credential
	 *            The password required to access the node (optional)
	 * @param keyFile
	 *            The private key file required to access the node  (optional)
	 * @param nodeName
	 *            A unique logical name for the node, optional.
	 */
	public CustomNodeImpl(final String providerId, final String id, final String privateIp,
                          final String hostName,
                          final String username,
			final String credential, final String keyFile, final String nodeName) {
		this.providerId = providerId;
		this.id = id;
        this.hostName = hostName;
		this.privateIp = privateIp;
		this.username = username;
		this.credential = credential;
		this.keyFile = keyFile;
		this.nodeName = nodeName;
	}

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
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
	public String getKeyFile() {
		return keyFile;
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
		return privateIp;
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
        return "CustomNode{" +
                "providerId='" + providerId + '\'' +
                ", id='" + id + '\'' +
                ", privateIp='" + privateIp + '\'' +
                ", hostName='" + hostName + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", group='" + group + '\'' +
                ", username='" + username + '\'' +
                ", credential='" + credential + '\'' +
                ", keyFile='" + keyFile + '\'' +
                ", loginPort=" + loginPort +
                '}';
    }

    @Override
    public int hashCode() {
        int result = providerId != null ? providerId.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (privateIp != null ? privateIp.hashCode() : 0);
        result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + (nodeName != null ? nodeName.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (credential != null ? credential.hashCode() : 0);
        result = 31 * result + (keyFile != null ? keyFile.hashCode() : 0);
        result = 31 * result + loginPort;
        return result;
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
        if (StringUtils.isNotBlank(getPrivateIp())) {
            if (getPrivateIp().equals(other.getPrivateIp())) return true;
        }
        if (StringUtils.isNotBlank(getHostName())) {
            if (getHostName().equals(other.getHostName())) return true;
        }
        return false;
	}

}
