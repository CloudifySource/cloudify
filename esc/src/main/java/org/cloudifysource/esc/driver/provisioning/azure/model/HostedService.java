/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 *
 */

@XmlRootElement(name = "HostedService")
@XmlType(propOrder = {"url" , "serviceName" })
public class HostedService {

	private String url;
	private String serviceName;
	
	@XmlElement(name = "Url")
	public String getUrl() {
		return url;
	}
	
	public void setUrl(final String url) {
		this.url = url;
	}
	
	@XmlElement(name = "ServiceName")
	public String getServiceName() {
		return serviceName;
	}
	
	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}
	
	
	
}
