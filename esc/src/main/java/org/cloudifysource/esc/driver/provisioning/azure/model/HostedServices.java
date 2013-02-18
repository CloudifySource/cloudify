/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author elip
 *
 */

@XmlRootElement(name = "HostedServices")
public class HostedServices implements Iterable<HostedService> {

	private List<HostedService> hostedServices = new ArrayList<HostedService>();

	@XmlElement(name = "HostedService")
	public List<HostedService> getHostedServices() {
		return hostedServices;
	}

	public void setHostedServices(final List<HostedService> hostedServices) {
		this.hostedServices = hostedServices;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<HostedService> iterator() {
		return hostedServices.iterator();
	}

	/**
	 * 
	 * @param storageServiceName .
	 * @return .
	 */
	public boolean contains(final String cloudServiceName) {
		
		for (HostedService service : hostedServices) {
			if (service.getServiceName().equals(cloudServiceName)) {
				return true;
			}
		}
		return false;
	}
	
	
}
