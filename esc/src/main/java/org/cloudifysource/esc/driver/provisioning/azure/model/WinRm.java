package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author mourouvi (fastconnect)
 *
 */

@XmlType(name="WinRm")
public class WinRm {
	
	private Listeners listeners;

	@XmlElement(name="Listeners")
	public Listeners getListeners() {
		return listeners;
	}

	public void setListeners(Listeners listeners) {
		this.listeners = listeners;
	}

}
