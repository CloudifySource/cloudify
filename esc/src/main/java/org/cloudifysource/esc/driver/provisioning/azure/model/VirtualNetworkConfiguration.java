package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "VirtualNetworkConfiguration")
public class VirtualNetworkConfiguration {
	
	private VirtualNetworkSites virtualNetworkSites;

	@XmlElement(name = "VirtualNetworkSites")
	public VirtualNetworkSites getVirtualNetworkSites() {
		return virtualNetworkSites;
	}

	public void setVirtualNetworkSites(final VirtualNetworkSites virtualNetworkSites) {
		this.virtualNetworkSites = virtualNetworkSites;
	}

}
