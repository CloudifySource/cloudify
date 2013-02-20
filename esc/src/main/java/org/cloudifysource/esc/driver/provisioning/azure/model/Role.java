package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "Role", propOrder = {
		"roleName", "roleType", "configurationSets", "availabilitySetName" , "osVirtualHardDisk",
		"roleSize" })
public class Role {

	private String roleName;
	private String roleType;
	private ConfigurationSets configurationSets;
	private String availabilitySetName;	
	private OSVirtualHardDisk osVirtualHardDisk;
	private String roleSize;

	@XmlElement(name = "RoleName")
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}

	@XmlElement(name = "RoleType")
	public String getRoleType() {
		return roleType;
	}

	public void setRoleType(final String roleType) {
		this.roleType = roleType;
	}

	@XmlElement(name = "ConfigurationSets")
	public ConfigurationSets getConfigurationSets() {
		return configurationSets;
	}

	public void setConfigurationSets(final ConfigurationSets configurationSets) {
		this.configurationSets = configurationSets;
	}

	@XmlElement(name = "OSVirtualHardDisk")
	public OSVirtualHardDisk getOsVirtualHardDisk() {
		return osVirtualHardDisk;
	}

	public void setOSVirtualHardDisk(final OSVirtualHardDisk osVirtualHardDisk) {
		this.osVirtualHardDisk = osVirtualHardDisk;
	}

	@XmlElement(name = "RoleSize")
	public String getRoleSize() {
		return roleSize;
	}

	public void setRoleSize(final String roleSize) {
		this.roleSize = roleSize;
	}
	
	@XmlElement(name = "AvailabilitySetName")
	public String getAvailabilitySetName() {
		return availabilitySetName;
	}

	public void setAvailabilitySetName(final String availabilitySetName) {
		this.availabilitySetName = availabilitySetName;
	}

}
