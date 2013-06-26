package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author mourouvi (fastconnect)
 *
 */

@XmlType(name="Listeners")
public class Listeners {
	
	private List<Listener> listeners = new ArrayList<Listener>();

	@XmlElement(name = "Listener")
	public List<Listener> getListeners() {
		return listeners;
	}

	public void setListeners(List<Listener> listeners) {
		this.listeners = listeners;
	}
	
	@Override
	public String toString() {
		return "Listeners [Listeners=" + listeners + "]";
	}
	
}
