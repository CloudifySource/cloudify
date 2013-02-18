package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlRootElement(name = "CreateAffinityGroup")
@XmlType(propOrder = {"name" , "label" , "description" , "location" })
public class CreateAffinityGroup {

	private String name;
	private String label;
	private String description;
	private String location;
	
	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	@XmlElement(name = "Label")
	public String getLabel() {
		return label;
	}
	
	public void setLabel(final String label) {
		this.label = label;
	}
	
	@XmlElement(name = "Description")
	public String getDescription() {
		return description;
	}
	
	public void setDescription(final String description) {
		this.description = description;
	}
	
	@XmlElement(name = "Location")
	public String getLocation() {
		return location;
	}
	
	public void setLocation(final String location) {
		this.location = location;
	}
}
