package org.cloudifysource.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.memory.UserAttribute;

public class CloudifyUserAttribute extends UserAttribute {

	private List<String> authGroups = new ArrayList<String>();
	
	/**
	 * Adds a new authorization group.
	 * @param newAuthGroup The name of the authorization group to add
	 */
    public void addAuthGroup(final String newAuthGroup) {
        authGroups.add(newAuthGroup);
    }

    /**
     * Gets all authorization groups of this user.
     * @return all authorization groups of this user
     */
    public List<String> getAuthGroups() {
        return authGroups;
    }

    /**
     * Set all authorization groups for this user.
     * @param authGroups The user's authorization groups
     */
    public void setAuthGroups(final List<String> authGroups) {
        this.authGroups = authGroups;
    }
}
