package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author mourouvi (fastconnect)
 *
 */

@XmlType(name="Listener")
public class Listener {
	
	private String type;
	private String certificateThumbprint;

	@XmlElement(required=true, name="Type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name="CertificateThumbprint")
	public String getCertificateThumbprint() {
		return certificateThumbprint;
	}
	public void setCertificateThumbprint(String certificateThumbprint) {
		this.certificateThumbprint = certificateThumbprint;
	}

}
