package org.cloudifysource.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;


public class CloudifyUser implements CloudifyUserDetails {

	private static final String VALUES_SEPARATOR = ",";
	private User delegateUser;
	private Collection<String> authGroups;

	private static final Logger logger = Logger.getLogger(CloudifyUser.class.getName());
	
	public CloudifyUser(final CloudifyUserDetails cloudifyUserDetails) {
		delegateUser = new User(cloudifyUserDetails.getUsername(), cloudifyUserDetails.getPassword(), 
				cloudifyUserDetails.getAuthorities());
		this.authGroups = cloudifyUserDetails.getAuthGroups();
		logger.warning("***** In CloudifyUser(CloudifyUserDetails) ctor, not good");
	}
	
	
	public CloudifyUser(final String username, final String password, final String roles, final String authGroups) {
		
		Collection<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		Set<String> roleNames = splitAndTrim(roles, VALUES_SEPARATOR);
		for (String role : roleNames) {
			grantedAuthorities.add(new SimpleGrantedAuthority(role));
		}		

		delegateUser = new User(username, password, grantedAuthorities);
		this.authGroups = splitAndTrim(authGroups, VALUES_SEPARATOR);
		
		logger.warning("***** In CloudifyUser full ctor; username: " + username + ", password: " + password 
				+ ", roles: " + roles + ", authGroups: " + authGroups);
	}
	
	
	private Set<String> splitAndTrim(final String delimitedValues, final String delimiter) {
		Set<String> valuesSet = new HashSet<String>();
		String[] valuesArr = StringUtils.split(delimitedValues, delimiter);
		
		for (String token : valuesArr) {
			valuesSet.add(token.trim());
		}

		return valuesSet;
	}


	public CloudifyUser(final String username, final String password, final Collection<GrantedAuthority> authorities,
			final Collection<String> authGroups) {
		delegateUser = new User(username, password, authorities);
		this.authGroups = authGroups;
	}
	

	@Override
	public Collection<String> getRoles() {
		Collection<String> roles = new ArrayList<String>();
		for (GrantedAuthority authority : this.getAuthorities()) {
			roles.add(authority.getAuthority());
		}
		
		return roles;
	}
	

	@Override
	public Collection<String> getAuthGroups() {
		return authGroups;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return delegateUser.getAuthorities();
	}

	@Override
	public String getPassword() {
		return delegateUser.getPassword();
	}

	@Override
	public String getUsername() {
		return delegateUser.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return delegateUser.isAccountNonExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return delegateUser.isAccountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return delegateUser.isCredentialsNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return delegateUser.isEnabled();
	}
	
	
	//TODO override toString, possibly also hashCode and equals

}
