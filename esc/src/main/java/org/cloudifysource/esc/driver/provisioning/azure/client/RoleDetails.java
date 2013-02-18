/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.client;

/**
 * @author elip
 *
 */
public class RoleDetails {
	
	private String privateIp;
	private String publicIp;
	private String id;
	
	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

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
	
	

}
