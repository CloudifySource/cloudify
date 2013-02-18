package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "AddressSpace")
public class AddressSpace {
	
	private String addressPrefix;

	@XmlElement(name = "AddressPrefix")
	public String getAddressPrefix() {
		return addressPrefix;
	}

	public void setAddressPrefix(final String addressPrefix) {
		this.addressPrefix = addressPrefix;
	}
	
	
}
