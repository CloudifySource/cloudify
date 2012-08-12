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

@XmlType(name = "AttachedTo", propOrder = { "hostedServiceName",
		"deploymentName", "roleName" })
public class AttachedTo {

	private String hostedServiceName;
	private String deploymentName;
	private String roleName;

	@XmlElement(name = "HostedServiceName")
	public String getHostedServiceName() {
		return hostedServiceName;
	}

	public void setHostedServiceName(final String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}

	@XmlElement(name = "DeploymentName")
	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(final String deploymentName) {
		this.deploymentName = deploymentName;
	}

	@XmlElement(name = "RoleName")
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}

}
