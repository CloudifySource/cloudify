package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "VirtualNetworkSites")
public class VirtualNetworkSites implements Iterable<VirtualNetworkSite> {
	
	private List<VirtualNetworkSite> virtualNetworkSites = new ArrayList<VirtualNetworkSite>();

	@Override
	public Iterator<VirtualNetworkSite> iterator() {
		return virtualNetworkSites.iterator();
	}

	@XmlElement(name = "VirtualNetworkSite")
	public List<VirtualNetworkSite> getVirtualNetworkSites() {
		return virtualNetworkSites;
	}

	public void setVirtualNetworkSites(final List<VirtualNetworkSite> virtualNetworkSites) {
		this.virtualNetworkSites = virtualNetworkSites;
	}
	
	/**
	 * 
	 * @param networkName . 
	 * @return .
	 */
	public boolean contains(final String networkName) {
		for (VirtualNetworkSite site : virtualNetworkSites) {
			if (site.getName().equals(networkName)) {
				return true;
			}
		}
		return false;
	}
}
