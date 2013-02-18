/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 * 
 */

@XmlType(name = "RoleInstance", propOrder = { "roleName", "instanceName",
		"instanceStatus", "instanceFaultDomain", "ipAddress" })
public class RoleInstance {

	private String roleName;
	private String instanceName;
	private String instanceStatus;
	private int instanceFaultDomain;
	private String ipAddress;
	
	@XmlElement(name = "RoleName")
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}
	@XmlElement(name = "InstanceName")
	public String getInstanceName() {
		return instanceName;
	}
	public void setInstanceName(final String instanceName) {
		this.instanceName = instanceName;
	}
	@XmlElement(name = "InstanceStatus")
	public String getInstanceStatus() {
		return instanceStatus;
	}
	public void setInstanceStatus(final String instanceStatus) {
		this.instanceStatus = instanceStatus;
	}
	
	@XmlElement(name = "InstanceFaultDomain")
	public int getInstanceFaultDomain() {
		return instanceFaultDomain;
	}
	public void setInstanceFaultDomain(final int instanceFaultDomain) {
		this.instanceFaultDomain = instanceFaultDomain;
	}
	
	@XmlElement(name = "IpAddress")
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(final String ipAddress) {
		this.ipAddress = ipAddress;
	}


}
