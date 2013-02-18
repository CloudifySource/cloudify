package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author elip
 * 
 */
@XmlRootElement(name = "NetworkConfiguration")
public class GlobalNetworkConfiguration {

	private VirtualNetworkConfiguration virtualNetworkConfiguration;

	@XmlElement(name = "VirtualNetworkConfiguration")
	public VirtualNetworkConfiguration getVirtualNetworkConfiguration() {
		return virtualNetworkConfiguration;
	}

	public void setVirtualNetworkConfiguration(
			final VirtualNetworkConfiguration virtualNetworkConfiguration) {
		this.virtualNetworkConfiguration = virtualNetworkConfiguration;
	}

}
