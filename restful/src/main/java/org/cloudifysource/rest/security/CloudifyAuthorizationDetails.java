package org.cloudifysource.rest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * POJO for holding the data needed for permission evaluation.
 * @author noak
 * 
 * @since 2.3.0
 */
public class CloudifyAuthorizationDetails {
	
	private String username;
	private Collection<String> roles = new ArrayList<String>();
	private Collection<String> authGroups = new ArrayList<String>();
	
	private Logger logger = java.util.logging.Logger.getLogger(CloudifyAuthorizationDetails.class.getName());
	
	public CloudifyAuthorizationDetails(final Authentication authentication) {
		
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			logger.warning("Anonymous user is not supported");
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
    	if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
    		logger.warning("Authentication object type not supported. "
    				+ "Verify your Spring configuration is valid.");
    		throw new AccessDeniedException("Authentication object type not supported. "
    				+ "Verify your Spring configuration is valid.");
    	}
    	
    	username = authentication.getName();
    	
    	//set roles
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			roles.add(authority.getAuthority());
		}
		
		//set auth groups
		if (authentication instanceof CustomAuthenticationToken) {
			authGroups = ((CustomAuthenticationToken) authentication).getAuthGroups();
		} else {
			authGroups.addAll(roles);
		}
	}

	public String getUsername() {
		return username;
	}

	public Collection<String> getRoles() {
		return roles;
	}

	public Collection<String> getAuthGroups() {
		return authGroups;
	}

}
