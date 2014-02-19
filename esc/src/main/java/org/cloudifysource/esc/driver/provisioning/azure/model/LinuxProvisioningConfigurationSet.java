package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */

@XmlType(propOrder = { "configurationSetType", "hostName", "userName",
		"userPassword", "disableSshPasswordAuthentication" })
public class LinuxProvisioningConfigurationSet extends ConfigurationSet {

	private String configurationSetType = ConfigurationSet.LINUX_PROVISIONING_CONFIGURATION;
	private String hostName;
	private String userName;
	private String userPassword;
	private boolean disableSshPasswordAuthentication;

    @XmlAttribute(name = "type")
    public String getType(){
        return "LinuxProvisioningConfigurationSet";
    }

	@XmlElement(name = "HostName")
	public String getHostName() {
		return hostName;
	}

	public void setHostName(final String hostName) {
		this.hostName = hostName;
	}

	@XmlElement(name = "UserName")
	public String getUserName() {
		return userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	@XmlElement(name = "UserPassword")
	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(final String userPassword) {
		this.userPassword = userPassword;
	}

	@XmlElement(name = "DisableSshPasswordAuthentication")
	public boolean isDisableSshPasswordAuthentication() {
		return disableSshPasswordAuthentication;
	}

	public void setDisableSshPasswordAuthentication(
			final boolean disableSshPasswordAuthentication) {
		this.disableSshPasswordAuthentication = disableSshPasswordAuthentication;
	}

	@XmlElement(name = "ConfigurationSetType")
	public String getConfigurationSetType() {
		return configurationSetType;
	}
}
