package org.cloudifysource.esc.driver.provisioning;

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

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}

	public String getPrivateAddress() {
		return privateAddress;
	}

	public void setPrivateAddress(String privateAddress) {
		this.privateAddress = privateAddress;
	}

	public String getPublicAddress() {
		return publicAddress;
	}

	public void setPublicAddress(String publicAddress) {
		this.publicAddress = publicAddress;
	}

	public boolean isCloudifyInstalled() {
		return cloudifyInstalled;
	}

	public void setCloudifyInstalled(boolean cloudifyInstalled) {
		this.cloudifyInstalled = cloudifyInstalled;
	}

	public boolean isAgentRunning() {
		return agentRunning;
	}

	public void setAgentRunning(boolean agentRunning) {
		this.agentRunning = agentRunning;
	}

	public String getInstallationDirectory() {
		return installationDirectory;
	}

	public void setInstallationDirectory(String installationDirectory) {
		this.installationDirectory = installationDirectory;
	}

	public String getClusterAddress() {
		return clusterAddress;
	}

	public void setClusterAddress(String clusterAddress) {
		this.clusterAddress = clusterAddress;
	}

	@Override
	public String toString() {
		return "MachineDetails [machineId=" + machineId + ", privateAddress="
				+ privateAddress + ", publicAddress=" + publicAddress
				+ ", clusterAddress=" + clusterAddress
				+ ", gigaspacesInstalled=" + cloudifyInstalled
				+ ", agentRunning=" + agentRunning + ", installationDirectory="
				+ installationDirectory + "]";
	}

	public boolean isUsePrivateAddress() {
		return usePrivateAddress;
	}

	public void setUsePrivateAddress(boolean usePrivateAddress) {
		this.usePrivateAddress = usePrivateAddress;
	}

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

	public void setRemoteUsername(String remoteUsername) {
		this.remoteUsername = remoteUsername;
	}

	public String getRemotePassword() {
		return remotePassword;
	}

	public void setRemotePassword(String remotePassword) {
		this.remotePassword = remotePassword;
	}

}
