/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 *
 */

@XmlType(name = "Deployments")
public class Deployments implements Iterable<Deployment> {
	
	private List<Deployment> deployments = new ArrayList<Deployment>();

	@XmlElement(name = "Deployment")
	public List<Deployment> getDeployments() {
		return deployments;
	}

	public void setDeployments(final List<Deployment> deployments) {
		this.deployments = deployments;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Deployment> iterator() {
		return deployments.iterator();
	}
	
	/**
	 * 
	 * @param deploymentName .
	 * @return .
	 */
	public boolean contains(final String deploymentName) {
		for (Deployment deployment : deployments) {
			if (deployment.getName().equals(deploymentName)) {
				return true;
			}
		}
		return false;
	}
	

}
