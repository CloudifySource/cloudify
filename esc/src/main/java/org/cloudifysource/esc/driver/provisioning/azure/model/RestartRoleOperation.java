package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 *
 */
@XmlRootElement(name = "RestartRoleOperation")
@XmlType(propOrder = {"operationType" })
public class RestartRoleOperation {

	private String operationType = "RestartRoleOperation";

	@XmlElement(name = "OperationType")
	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(final String operationType) {
		this.operationType = operationType;
	}
	
}
