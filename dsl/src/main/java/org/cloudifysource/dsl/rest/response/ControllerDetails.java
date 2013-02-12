package org.cloudifysource.dsl.rest.response;

public class ControllerDetails {

	private String privateIp;
	private String publicIp;
	private int instanceId;
	private boolean bootstrapToPublicIp;

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(final String privateIp) {
		this.privateIp = privateIp;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(final String publicIp) {
		this.publicIp = publicIp;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(final int instanceId) {
		this.instanceId = instanceId;
	}

	public boolean isBootstrapToPublicIp() {
		return bootstrapToPublicIp;
	}

	public void setBootstrapToPublicIp(final boolean bootstrapToPublicIp) {
		this.bootstrapToPublicIp = bootstrapToPublicIp;
	}

}
