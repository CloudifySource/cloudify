package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlRootElement(name = "CreateHostedService")
@XmlType(propOrder = {"serviceName" , "label" , "description" , "affinityGroup" })
public class CreateHostedService {

	private String serviceName;
	private String label;
	private String description;
	private String affinityGroup;

	@XmlElement(name = "ServiceName")
	public String getServiceName() {
		return serviceName;
	}
	
	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
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

	@XmlElement(name = "AffinityGroup")
	public String getAffinityGroup() {
		return affinityGroup;
	}
	
	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}
}
