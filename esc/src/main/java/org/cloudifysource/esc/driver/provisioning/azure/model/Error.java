package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlRootElement(name = "Error")
@XmlType(propOrder = {"code" , "message" })
public class Error {

	private String code;
	private String message;
	
	@XmlElement(name = "Code")
	public String getCode() {
		return code;
	}
	
	public void setCode(final String code) {
		this.code = code;
	}
	
	@XmlElement(name = "Message")
	public String getMessage() {
		return message;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	
}
