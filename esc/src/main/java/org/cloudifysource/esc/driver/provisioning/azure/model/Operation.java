package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlRootElement(name = "Operation")
@XmlType(propOrder = {"id" , "status" , "statusCode" , "error" })
public class Operation {

	private String id;
	private String status;
	private String statusCode;
	private Error error;
	
	@XmlElement(name = "ID")
	public String getId() {
		return id;
	}
	
	public void setId(final String id) {
		this.id = id;
	}
	
	@XmlElement(name = "Status")
	public String getStatus() {
		return status;
	}
	
	public void setStatus(final String status) {
		this.status = status;
	}
	
	@XmlElement(name = "HttpStatusCode")
	public String getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(final String statusCode) {
		this.statusCode = statusCode;
	}
	
	@XmlElement(name = "Error")
	public Error getError() {
		return error;
	}
	
	public void setError(final Error error) {
		this.error = error;
	}
	
	
}
