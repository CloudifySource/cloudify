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
	
	private String protocol;
	private String certificateThumbprint;

	@XmlElement(required=true, name="Protocol")
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	@XmlElement(name="CertificateThumbprint")
	public String getCertificateThumbprint() {
		return certificateThumbprint;
	}
	public void setCertificateThumbprint(String certificateThumbprint) {
		this.certificateThumbprint = certificateThumbprint;
	}

}
