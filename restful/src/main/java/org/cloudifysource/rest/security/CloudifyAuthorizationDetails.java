package org.cloudifysource.rest.security;

import java.util.ArrayList;
import java.util.Collection;

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
	
	public CloudifyAuthorizationDetails(final Authentication authentication) {
		
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
    	if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
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
