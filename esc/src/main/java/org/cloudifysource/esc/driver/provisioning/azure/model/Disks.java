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

@XmlRootElement(name = "Disks")
public class Disks implements Iterable<Disk> {

	private List<Disk> disks = new ArrayList<Disk>();
	
	@XmlElement(name = "Disk")
	public List<Disk> getDisks() {
		return disks;
	}

	public void setDisks(final List<Disk> disks) {
		this.disks = disks;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Disk> iterator() {
		return disks.iterator();
	}
	
	/**
	 * 
	 * @param storageServiceName .
	 * @return .
	 */
	public boolean contains(final String diskName) {
		
		for (Disk disk: disks) {
			if (disk.getName().equals(diskName)) {
				return true;
			}
		}
		return false;
	}
}
