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

@XmlRootElement(name = "StorageServices")
public class StorageServices implements Iterable<StorageService> {
	
	private List<StorageService> storageServices = new ArrayList<StorageService>();

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<StorageService> iterator() {
		return storageServices.iterator();
	}

	@XmlElement(name = "StorageService")
	public List<StorageService> getStorageServices() {
		return storageServices;
	}

	public void setStorageServices(final List<StorageService> storageServices) {
		this.storageServices = storageServices;
	}	
	
	/**
	 * 
	 * @param storageServiceName .
	 * @return .
	 */
	public boolean contains(final String storageServiceName) {
		
		for (StorageService service : storageServices) {
			if (service.getServiceName().equals(storageServiceName)) {
				return true;
			}
		}
		return false;
	}
}
