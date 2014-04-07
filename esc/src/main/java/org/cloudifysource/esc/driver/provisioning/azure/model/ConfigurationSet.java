package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * @author elip
 *
 */

@XmlJavaTypeAdapter(ConfigurationSetAdapter.class)
public abstract class ConfigurationSet {
	
	public static final String WINDOWS_PROVISIONING_CONFIGURATION = "WindowsProvisioningConfiguration";
	public static final String LINUX_PROVISIONING_CONFIGURATION = "LinuxProvisioningConfiguration";
	public static final String NETWORK_PROVISIONING_CONFIGURATION = "NetworkConfiguration";
}
