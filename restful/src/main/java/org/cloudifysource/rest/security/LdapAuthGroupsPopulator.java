package org.cloudifysource.rest.security;

import java.util.Collection;

import org.springframework.ldap.core.DirContextOperations;

/**
 * Obtains a list of authorization groups for an Ldap user.
 * <p>
 * Used by the <tt>CustomLdapAuthenticationProvider</tt> once a user has been
 * authenticated to create the final user details object.
 * </p>
 *
 * @author noak
 * @since 2.3.0
 */
public interface LdapAuthGroupsPopulator {
	
    /**
     * Get the list of authorization groups of the user.
     *
     * @param user the user who's authorities are required
     * @param username the user name.
     *
     * @return the authorization groups of the given user.
     *
     */
	Collection<String> getAuthGroups(final DirContextOperations user, final String username);

}
