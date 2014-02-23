/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author elip
 * 
 */
public class ConfigurationSetAdapter
		extends
		XmlAdapter<ConfigurationSetAdapter.AdaptedConfigurationSet, ConfigurationSet> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
	 */
	@Override
	public ConfigurationSet unmarshal(
			final AdaptedConfigurationSet adaptedConfigurationSet)
			throws Exception {
		if (adaptedConfigurationSet == null) {
			return null;
		}

		
		if (ConfigurationSet.LINUX_PROVISIONING_CONFIGURATION.equals(adaptedConfigurationSet.configurationSetType)) {
			
			LinuxProvisioningConfigurationSet linuxProvisioningConfigurationSet = 
					new LinuxProvisioningConfigurationSet();
			linuxProvisioningConfigurationSet
					.setDisableSshPasswordAuthentication(adaptedConfigurationSet.disableSshPasswordAuthentication);
			linuxProvisioningConfigurationSet
					.setHostName(adaptedConfigurationSet.hostName);
			linuxProvisioningConfigurationSet
					.setUserName(adaptedConfigurationSet.userName);
			linuxProvisioningConfigurationSet
					.setUserPassword(adaptedConfigurationSet.userPassword);
			
		} else if (ConfigurationSet.WINDOWS_PROVISIONING_CONFIGURATION.equals(adaptedConfigurationSet.configurationSetType)) {
			WindowsProvisioningConfigurationSet windowsProvisioningConfigurationSet = 
					new WindowsProvisioningConfigurationSet();
//			windowsProvisioningConfigurationSet
//					.setDisableSshPasswordAuthentication(adaptedConfigurationSet.disableSshPasswordAuthentication);
//			windowsProvisioningConfigurationSet
//					.setHostName(adaptedConfigurationSet.hostName);
//			windowsProvisioningConfigurationSet
//					.setUserName(adaptedConfigurationSet.userName);
//			windowsProvisioningConfigurationSet
//					.setUserPassword(adaptedConfigurationSet.userPassword);

			windowsProvisioningConfigurationSet
					.setComputerName(adaptedConfigurationSet.computerName);
			windowsProvisioningConfigurationSet.setAdminUsername(adaptedConfigurationSet.adminUsername);
			windowsProvisioningConfigurationSet.setAdminPassword(adaptedConfigurationSet.adminPassword);
			windowsProvisioningConfigurationSet.setWinRm(adaptedConfigurationSet.winRm);
			
		} else {// NetworkConfiguration
			NetworkConfigurationSet networkConfigurationSet = new NetworkConfigurationSet();
			networkConfigurationSet
					.setConfigurationSetType(adaptedConfigurationSet.configurationSetType);
			networkConfigurationSet
					.setInputEndpoints(adaptedConfigurationSet.inputEndpoints);
			return networkConfigurationSet;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
	 */
	@Override
	public AdaptedConfigurationSet marshal(final ConfigurationSet configurationSet)
			throws Exception {

		if (configurationSet == null) {
			return null;
		}

		AdaptedConfigurationSet adaptedConfigurationSet = new AdaptedConfigurationSet();
		if (configurationSet instanceof LinuxProvisioningConfigurationSet) {
			LinuxProvisioningConfigurationSet linuxProvisioningConfigurationSet
								= (LinuxProvisioningConfigurationSet) configurationSet;

            adaptedConfigurationSet.type = linuxProvisioningConfigurationSet.getType();
			adaptedConfigurationSet.configurationSetType = linuxProvisioningConfigurationSet
					.getConfigurationSetType();
			adaptedConfigurationSet.disableSshPasswordAuthentication = linuxProvisioningConfigurationSet
					.isDisableSshPasswordAuthentication();
			adaptedConfigurationSet.hostName = linuxProvisioningConfigurationSet
					.getHostName();
			adaptedConfigurationSet.userName = linuxProvisioningConfigurationSet
					.getUserName();
			adaptedConfigurationSet.userPassword = linuxProvisioningConfigurationSet
					.getUserPassword();
		} else if (configurationSet instanceof WindowsProvisioningConfigurationSet) {
			
			WindowsProvisioningConfigurationSet WindowsProvisioningConfigurationSet = (WindowsProvisioningConfigurationSet) configurationSet;
            adaptedConfigurationSet.type = WindowsProvisioningConfigurationSet.getType();
			adaptedConfigurationSet.configurationSetType = WindowsProvisioningConfigurationSet.getConfigurationSetType();
//			adaptedConfigurationSet.disableSshPasswordAuthentication = WindowsProvisioningConfigurationSet.isDisableSshPasswordAuthentication();
//			adaptedConfigurationSet.hostName = WindowsProvisioningConfigurationSet.getHostName();
//			adaptedConfigurationSet.userName = WindowsProvisioningConfigurationSet.getUserName();
//			adaptedConfigurationSet.userPassword = WindowsProvisioningConfigurationSet.getUserPassword();
			adaptedConfigurationSet.adminUsername = WindowsProvisioningConfigurationSet.getAdminUsername();
			adaptedConfigurationSet.adminPassword = WindowsProvisioningConfigurationSet.getAdminPassword();
			adaptedConfigurationSet.computerName = WindowsProvisioningConfigurationSet.getComputerName();
			adaptedConfigurationSet.winRm = WindowsProvisioningConfigurationSet.getWinRm();
			
		} else {
			NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
			adaptedConfigurationSet.inputEndpoints = networkConfigurationSet
					.getInputEndpoints();
			adaptedConfigurationSet.configurationSetType = networkConfigurationSet
					.getConfigurationSetType();
		}

		return adaptedConfigurationSet;
	}
	
	/**
	 * 
	 * @author elip
	 *
	 */

    static class AdaptedConfigurationSet {

        @XmlAttribute(name="type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
        private String type;

		@XmlElement(name = "ConfigurationSetType")
		private String configurationSetType;


		@XmlElement(name="ComputerName")
		private String computerName;

        @XmlElement(name="AdminPassword")
		private String adminPassword;
		
		@XmlElement(name="WinRm")
		private WinRm winRm;

        @XmlElement(name="AdminUsername")
        private String adminUsername;

        @XmlElement(name = "HostName")
		private String hostName;

		@XmlElement(name = "UserName")
		private String userName;

		@XmlElement(name = "UserPassword")
		private String userPassword;

		@XmlElement(name = "DisableSshPasswordAuthentication")
		private boolean disableSshPasswordAuthentication;

		@XmlElement(name = "InputEndpoints")
		private InputEndpoints inputEndpoints;

	}
}
