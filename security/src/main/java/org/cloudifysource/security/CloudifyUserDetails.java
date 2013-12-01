package org.cloudifysource.security;

import java.util.Collection;

import org.springframework.security.core.userdetails.UserDetails;

public interface CloudifyUserDetails extends UserDetails {

    /**
     * Gets the permitted security roles for the active user.
     *
     * @return A collection of roles as string values.
     */
    Collection<String> getRoles();

    /**
     * Gets the registered authorization groups for the active user.
     *
     * @return A collection of authorization groups as string values.
     */
    Collection<String> getAuthGroups();
}
