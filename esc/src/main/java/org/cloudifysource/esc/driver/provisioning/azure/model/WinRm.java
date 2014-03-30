package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * WinRM model
 * @author mourouvi (fastconnect)
 *
 */
@XmlType(name= "WinRM")
public class WinRM {

	private Listeners listeners;

	@XmlElement(name="Listeners")
	public Listeners getListeners() {
		return listeners;
	}

	public void setListeners(Listeners listeners) {
		this.listeners = listeners;
	}
}
