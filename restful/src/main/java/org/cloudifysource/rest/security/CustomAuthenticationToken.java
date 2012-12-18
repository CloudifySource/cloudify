package org.cloudifysource.rest.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Extends {@link UsernamePasswordAuthenticationToken} and adds support for authorization groups.
 * @author noak
 * @since 2.3.0
 */
public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken implements ExtendedAuthentication {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -6592305155032221704L;
	private Collection<String> authGroups;
	private static final Logger logger = java.util.logging.Logger.getLogger(CustomAuthenticationToken.class.getName());
	
	/**
	 * Ctor.
	 */
	public CustomAuthenticationToken(final Object principal, final Object credentials, 
			final Collection<? extends GrantedAuthority> authorities, final Collection <String> authGroups) {
		super(principal, credentials, authorities);
		logger.finest("CustomAuthenticationToken : constructor");
		//set authGroups
		if (authGroups == null) {
            this.authGroups = new ArrayList<String>();
            return;
        }
		
		logger.finest("Setting auth groups " + Arrays.toString(authGroups.toArray()) + " for user.");

        for (String authGroup: authGroups) {
            if (authGroup == null) {
                throw new IllegalArgumentException("Authorization groups collection cannot contain any null elements");
            }
        }
        ArrayList<String> temp = new ArrayList<String>(authGroups.size());
        temp.addAll(authGroups);
        this.authGroups = Collections.unmodifiableList(temp);
	}

	@Override
	public Collection<String> getAuthGroups() {
		return authGroups;
	}

}
