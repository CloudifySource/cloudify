package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "RoleList")
public class RoleList implements Iterable<Role> {

	private List<Role> roles = new ArrayList<Role>();
	
	@Override
	public Iterator<Role> iterator() {
		return roles.iterator();
	}

	@XmlElement(name = "Role")
	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(final List<Role> roles) {
		this.roles = roles;
	}
}
