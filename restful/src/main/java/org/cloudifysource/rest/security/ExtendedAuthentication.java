package org.cloudifysource.rest.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;

/**
 * Extends the {@link Authentication} interface, adding the AuthGroups to it.
 * @author noak
 * @since 2.3.0 *
 */
public interface ExtendedAuthentication extends Authentication {
	
    /**
     * Set by an <code>AuthenticationManager</code> to indicate the authorization groups that the principal belongs to.
     * Note that classes should not rely on this value as being valid unless it has been set by a trusted
     * <code>AuthenticationManager</code>.
     * <p>
     * Implementations should ensure that modifications to the returned collection
     * array do not affect the state of the Authentication object, or use an unmodifiable instance.
     * </p>
     *
     * @return the authorization groups the principal belongs to, or an empty collection if the token has not 
     * been authenticated.
     * Never null.
     */
    Collection<String> getAuthGroups();

}
