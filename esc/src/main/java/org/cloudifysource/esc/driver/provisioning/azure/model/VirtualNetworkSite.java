package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "VirtualNetworkSite", propOrder = {"addressSpace" })
public class VirtualNetworkSite {
	
	private String name;
	private String affinityGroup;
	private AddressSpace addressSpace;
	
	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	@XmlAttribute(name = "AffinityGroup")	
	public String getAffinityGroup() {
		return affinityGroup;
	}
	
	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}
		
	@XmlElement(name = "AddressSpace")
	public AddressSpace getAddressSpace() {
		return addressSpace;
	}

	public void setAddressSpace(final AddressSpace addressSpace) {
		this.addressSpace = addressSpace;
	}
}
