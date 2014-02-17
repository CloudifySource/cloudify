package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlRootElement(name = "Deployment")
@XmlType(propOrder = { "name", "deploymentSlot", "deploymentName", "privateId", "status",
		"label" , "roleInstanceList" , "roleList", "virtualNetworkName" , "hostedServiceName" })
public class Deployment {

	private String name;
	private String deploymentSlot;
	private String deploymentName;
	private String privateId;
	private String status;
	private String label;
	private RoleInstanceList roleInstanceList;
	private RoleList roleList;
	private String virtualNetworkName;
	
	// not azure model
	private String hostedServiceName;

	public String getHostedServiceName() {
		return hostedServiceName;
	}

	public void setHostedServiceName(final String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}

	@XmlElement(name = "RoleInstanceList")
	public RoleInstanceList getRoleInstanceList() {
		return roleInstanceList;
	}

	public void setRoleInstanceList(final RoleInstanceList roleInstanceList) {
		this.roleInstanceList = roleInstanceList;
	}
	
	@XmlElement(name = "VirtualNetworkName")
	public String getVirtualNetworkName() {
		return virtualNetworkName;
	}

	public void setVirtualNetworkName(final String virtualNetworkName) {
		this.virtualNetworkName = virtualNetworkName;
	}

	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@XmlElement(name = "DeploymentSlot")
	public String getDeploymentSlot() {
		return deploymentSlot;
	}

	public void setDeploymentSlot(final String slot) {
		this.deploymentSlot = slot;
	}
	
	@XmlElement(name = "DeploymentName")
	public String getDeploymentName() {
		return deploymentName;
	}
	
	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}
	
	@XmlElement(name = "PrivateID")
	public String getPrivateId() {
		return privateId;
	}

	public void setPrivateId(final String privateId) {
		this.privateId = privateId;
	}


	@XmlElement(name = "Status")
	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	@XmlElement(name = "Label")
	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	@XmlElement(name = "RoleList")
	public RoleList getRoleList() {
		return roleList;
	}

	public void setRoleList(final RoleList roleList) {
		this.roleList = roleList;
	}
}
