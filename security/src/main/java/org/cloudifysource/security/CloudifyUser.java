package org.cloudifysource.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;


public class CloudifyUser implements CloudifyUserDetails {

	private static final String VALUES_SEPARATOR = ",";
	private User delegateUser;
	private Collection<String> authGroups;
	
	
	public CloudifyUser(final CloudifyUserDetails cloudifyUserDetails) {
		delegateUser = new User(cloudifyUserDetails.getUsername(), cloudifyUserDetails.getPassword(), 
				cloudifyUserDetails.getAuthorities());
		this.authGroups = cloudifyUserDetails.getAuthGroups();
	}
	
	
	public CloudifyUser(final String username, final String password, final String roles, final String authGroups) {
		
		Collection<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		Set<String> roleNames = splitAndTrim(roles, VALUES_SEPARATOR);
		for (String role : roleNames) {
			grantedAuthorities.add(new SimpleGrantedAuthority(role));
		}		

		delegateUser = new User(username, password, grantedAuthorities);
		this.authGroups = splitAndTrim(authGroups, VALUES_SEPARATOR);
	}
	
	
	private static Set<String> splitAndTrim(final String delimitedValues, final String delimiter) {
		Set<String> valuesSet = new HashSet<String>();
		String[] valuesArr = StringUtils.split(delimitedValues, delimiter);
		
		for (String token : valuesArr) {
			valuesSet.add(token.trim());
		}

		return valuesSet;
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
	
	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");
        sb.append("Username: ").append(getUsername()).append("; ");
        sb.append("Password: [PROTECTED]; ");
        sb.append("Enabled: ").append(isEnabled()).append("; ");
        sb.append("AccountNonExpired: ").append(isAccountNonExpired()).append("; ");
        sb.append("credentialsNonExpired: ").append(isCredentialsNonExpired()).append("; ");
        sb.append("AccountNonLocked: ").append(isAccountNonLocked()).append("; ");

        Collection<? extends GrantedAuthority> authorities = getAuthorities();
        if (!authorities.isEmpty()) {
            sb.append("Granted Authorities: ");

            boolean first = true;
            for (GrantedAuthority role : authorities) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(role);
            }
        } else {
            sb.append("Not granted any authorities (roles)");
        }
        
        sb.append("; ");
        
        Collection<String> authGroups = getAuthGroups();
        if (!authGroups.isEmpty()) {
            sb.append("Authorization Groups: ");

            boolean first = true;
            for (String authGroup : authGroups) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(authGroup);
            }
        } else {
            sb.append("Not granted any aothorization groups");
        }

        return sb.toString();
    }

}
