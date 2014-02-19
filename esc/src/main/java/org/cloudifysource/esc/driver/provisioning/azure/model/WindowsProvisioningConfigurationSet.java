package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author mourouvi (fastconnect)
 *
 */

@XmlType(propOrder = { "configurationSetType", "computerName", "adminUsername", "winRm", "adminPassword"})
public class WindowsProvisioningConfigurationSet extends ConfigurationSet {

	private String configurationSetType = ConfigurationSet.WINDOWS_PROVISIONING_CONFIGURATION;
	private String hostName;
//	private String userName;
//	private String userPassword;
    private String adminUsername;
	private String adminPassword;
	private String computerName;
	private WinRm winRm;
//	private boolean disableSshPasswordAuthentication;

    @XmlAttribute(name = "type")
    public String getType(){
        return "WindowsProvisioningConfigurationSet";
    }

//	@XmlElement(name = "HostName")
//	public String getHostName() {
//		return hostName;
//	}
//
//	public void setHostName(final String hostName) {
//		this.hostName = hostName;
//	}
//
//	@XmlElement(name = "UserName")
//	public String getUserName() {
//		return userName;
//	}
//
//	public void setUserName(final String userName) {
//		this.userName = userName;
//	}
//
//	@XmlElement(name = "UserPassword")
//	public String getUserPassword() {
//		return userPassword;
//	}
//
//	public void setUserPassword(final String userPassword) {
//		this.userPassword = userPassword;
//	}

//	@XmlElement(name = "DisableSshPasswordAuthentication")
//	public boolean isDisableSshPasswordAuthentication() {
//		return disableSshPasswordAuthentication;
//	}
//
//	public void setDisableSshPasswordAuthentication(
//			final boolean disableSshPasswordAuthentication) {
//		this.disableSshPasswordAuthentication = disableSshPasswordAuthentication;
//	}

	@XmlElement(name = "ConfigurationSetType")
	public String getConfigurationSetType() {
		return configurationSetType;
	}

    @XmlElement(name = "AdminUsername")
    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(final String adminUsername) { this.adminUsername = adminUsername; }

	@XmlElement(name="AdminPassword")
	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	@XmlElement(name="ComputerName")
	public String getComputerName() {
		return computerName;
	}

	public void setComputerName(String computerName) {
		this.computerName = computerName;
	}

	@XmlElement(name="WinRm")
	public WinRm getWinRm() {
		return winRm;
	}

	public void setWinRm(WinRm winRm) {
		this.winRm = winRm;
	}
}
