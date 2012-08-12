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

@XmlType(name = "StorageService", propOrder = {"url" , "serviceName" })
public class StorageService {
	
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
