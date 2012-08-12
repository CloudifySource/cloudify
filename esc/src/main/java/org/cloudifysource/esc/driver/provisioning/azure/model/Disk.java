/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 *
 */

@XmlType(name = "Disk", propOrder = {"affinityGroup" , "attachedTo" , "name" })
public class Disk {
	
	private String affinityGroup;
	private AttachedTo attachedTo;
	private String name;
	
	@XmlElement(name = "AffinityGroup")
	public String getAffinityGroup() {
		return affinityGroup;
	}
	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}
	
	@XmlElement(name = "AttachedTo")
	public AttachedTo getAttachedTo() {
		return attachedTo;
	}
	public void setAttachedTo(final AttachedTo attachedTo) {
		this.attachedTo = attachedTo;
	}
	
	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	
	

}
