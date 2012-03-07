/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

/*******
 * Described a Machine started by a cloud driver.
 * 
 * @author barakme
 * @since 2.0.0
 *
 */
public class MachineDetails {

	private String privateAddress;
	private String publicAddress;
	private String clusterAddress; // TODO: WTF IS THIS
	private boolean usePrivateAddress = true;

	private boolean cloudifyInstalled = false;
	private String installationDirectory = null;
	private boolean agentRunning = false;

	private String remoteUsername;
	private String remotePassword;

	private String machineId;

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(final String machineId) {
		this.machineId = machineId;
	}

	public String getPrivateAddress() {
		return privateAddress;
	}

	public void setPrivateAddress(final String privateAddress) {
		this.privateAddress = privateAddress;
	}

	public String getPublicAddress() {
		return publicAddress;
	}

	public void setPublicAddress(final String publicAddress) {
		this.publicAddress = publicAddress;
	}

	public boolean isCloudifyInstalled() {
		return cloudifyInstalled;
	}

	public void setCloudifyInstalled(final boolean cloudifyInstalled) {
		this.cloudifyInstalled = cloudifyInstalled;
	}

	public boolean isAgentRunning() {
		return agentRunning;
	}

	public void setAgentRunning(final boolean agentRunning) {
		this.agentRunning = agentRunning;
	}

	public String getInstallationDirectory() {
		return installationDirectory;
	}

	public void setInstallationDirectory(final String installationDirectory) {
		this.installationDirectory = installationDirectory;
	}

	@Deprecated
	public String getClusterAddress() {
		return clusterAddress;
	}

	@Deprecated
	public void setClusterAddress(final String clusterAddress) {
		this.clusterAddress = clusterAddress;
	}

	@Override
	public String toString() {
		return "MachineDetails [machineId=" + machineId + ", privateAddress=" + privateAddress + ", publicAddress="
				+ publicAddress + ", clusterAddress=" + clusterAddress + ", gigaspacesInstalled=" + cloudifyInstalled
				+ ", agentRunning=" + agentRunning + ", installationDirectory=" + installationDirectory + "]";
	}

	public boolean isUsePrivateAddress() {
		return usePrivateAddress;
	}

	//TODO - this flag should be removed - it should only be set in the adapter, not in the driver.
	public void setUsePrivateAddress(final boolean usePrivateAddress) {
		this.usePrivateAddress = usePrivateAddress;
	}

	/*****
	 * Returns the IP that should be used for cluster communications.
	 * @return the IP.
	 */
	public String getIp() {
		if (this.isUsePrivateAddress()) {
			return this.getPrivateAddress();

		} else {
			return this.getPublicAddress();
		}
	}

	public String getRemoteUsername() {
		return remoteUsername;
	}

	public void setRemoteUsername(final String remoteUsername) {
		this.remoteUsername = remoteUsername;
	}

	public String getRemotePassword() {
		return remotePassword;
	}

	public void setRemotePassword(final String remotePassword) {
		this.remotePassword = remotePassword;
	}

}
