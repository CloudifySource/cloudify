/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author elip
 *
 */

@XmlRootElement(name = "StartRoleOperation")
public class StartRoleOperation {
	
	private String operationType = "StartRoleOperation";

	@XmlElement(name = "OperationType")
	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(final String operationType) {
		this.operationType = operationType;
	}
}
