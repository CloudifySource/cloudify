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

@XmlRootElement(name = "AffinityGroups")
public class AffinityGroups implements Iterable<AffinityGroup> {
	
	private List<AffinityGroup> affinityGroups = new ArrayList<AffinityGroup>();

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<AffinityGroup> iterator() {
		return affinityGroups.iterator();
	}

	@XmlElement(name = "AffinityGroup")
	public List<AffinityGroup> getAffinityGroups() {
		return affinityGroups;
	}
	
	public void setAffinityGroups(final List<AffinityGroup> affinityGroups) {
		this.affinityGroups = affinityGroups;
	}
	
	/**
	 * 
	 * @param affinityGroupName .
	 * @return .
	 */
	public boolean contains(final String affinityGroupName) {
		for (AffinityGroup group : affinityGroups) {
			if (group.getName().equals(affinityGroupName)) {
				return true;
			}
		}
		return false;
	}
}
