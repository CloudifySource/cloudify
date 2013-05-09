/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.securityldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.Name;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.ldap.ppolicy.PasswordPolicyData;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.util.Assert;

/**
 * Extends Spring default implementation for LdapUserDetailsImpl to add groups.
 * @author noak
 *
 */
public class ExtendedLdapUserDetailsImpl implements LdapUserDetails, PasswordPolicyData {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -5094299715539461256L;
	
	 //~ Instance fields ==============================================================================================

    private String dn;
    private String password;
    private String username;
    private Collection<GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;
    private Collection<String> authGroups = new ArrayList<String>();
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    // PPolicy data
    private int timeBeforeExpiration = Integer.MAX_VALUE;
    private int graceLoginsRemaining = Integer.MAX_VALUE;

    //~ Constructors ===================================================================================================

    protected ExtendedLdapUserDetailsImpl() { }

    //~ Methods ========================================================================================================

    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    public Collection<String> getAuthGroups() {
        return authGroups;
    }

    public String getDn() {
        return dn;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTimeBeforeExpiration() {
        return timeBeforeExpiration;
    }

    public int getGraceLoginsRemaining() {
        return graceLoginsRemaining;
    }

    /**
     * Returns this object's representation as String.
     * @return this object's representation as String
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");
        sb.append("Username: ").append(this.username).append("; ");
        sb.append("Password: [PROTECTED]; ");
        sb.append("Enabled: ").append(this.enabled).append("; ");
        sb.append("AccountNonExpired: ").append(this.accountNonExpired).append("; ");
        sb.append("credentialsNonExpired: ").append(this.credentialsNonExpired).append("; ");
        sb.append("AccountNonLocked: ").append(this.accountNonLocked).append("; ");

        if (this.getAuthorities() != null) {
            sb.append("Granted Authorities: ");
            boolean first = true;

            for (Object authority : this.getAuthorities()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }

                sb.append(authority.toString());
            }
        } else {
            sb.append("Not granted any authorities");
        }
        
        if (this.getAuthGroups() != null) {
            sb.append("Granted Authorization Groups: ");
            boolean first = true;

            for (Object authGroup : this.getAuthGroups()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }

                sb.append(authGroup);
            }
        } else {
            sb.append("Not granted any authorization groups");
        }

        return sb.toString();
    }

    //~ Inner Classes ==================================================================================================

    /**
     * Variation of essence pattern. Used to create mutable intermediate object
     */
    public static class ExtendedEssence {
        protected ExtendedLdapUserDetailsImpl instance = createTarget();
        private List<GrantedAuthority> mutableAuthorities = new ArrayList<GrantedAuthority>();
        private List<String> mutableAuthGroups = new ArrayList<String>();

        public ExtendedEssence() { }

        public ExtendedEssence(final DirContextOperations ctx) {
            setDn(ctx.getDn());
        }

        public ExtendedEssence(final ExtendedLdapUserDetailsImpl copyMe) {
            setDn(copyMe.getDn());
            setUsername(copyMe.getUsername());
            setPassword(copyMe.getPassword());
            setEnabled(copyMe.isEnabled());
            setAccountNonExpired(copyMe.isAccountNonExpired());
            setCredentialsNonExpired(copyMe.isCredentialsNonExpired());
            setAccountNonLocked(copyMe.isAccountNonLocked());
            setAuthorities(copyMe.getAuthorities());
            setAuthGroups(copyMe.getAuthGroups());
        }

        /**
         * Create a target as an empty ExtendedLdapUserDetailsImpl.
         * @return ExtendedLdapUserDetailsImpl
         */
        protected ExtendedLdapUserDetailsImpl createTarget() {
            return new ExtendedLdapUserDetailsImpl();
        }

        /**
         * Adds the authority to the list, unless it is already there, in which case it is ignored.
         * @param authority authority to add.
         */
        public void addAuthority(final GrantedAuthority authority) {
            if (!hasAuthority(authority)) {
                mutableAuthorities.add(authority);
            }
        }

        private boolean hasAuthority(final GrantedAuthority newAuthority) {
            for (GrantedAuthority authority : mutableAuthorities) {
                if (authority.equals(newAuthority)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Adds the authorization group to the list, unless it is already there, in which case it is ignored.
         * @param authGroup authorization group to add.
         */
        public void addAuthGroup(final String authGroup) {
            if (!hasAuthGroup(authGroup)) {
                mutableAuthGroups.add(authGroup);
            }
        }

        private boolean hasAuthGroup(final String newAuthGroup) {
            for (String authGroup : mutableAuthGroups) {
                if (authGroup.equalsIgnoreCase(newAuthGroup)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * creates a user details object with authorities and groups.
         * @return user details as ExtendedLdapUserDetails
         */
        public ExtendedLdapUserDetailsImpl createUserDetails() {
            Assert.notNull(instance, "Essence can only be used to create a single instance");
            Assert.notNull(instance.username, "username must not be null");
            Assert.notNull(instance.getDn(), "Distinguished name must not be null");

            instance.authorities = Collections.unmodifiableList(mutableAuthorities);
            instance.authGroups = Collections.unmodifiableList(mutableAuthGroups);

            ExtendedLdapUserDetailsImpl newInstance = instance;

            instance = null;

            return newInstance;
        }

        public Collection<GrantedAuthority> getGrantedAuthorities() {
            return mutableAuthorities;
        }
        
        public Collection<String> getGrantedAuthGroups() {
            return mutableAuthGroups;
        }

        public void setAccountNonExpired(final boolean accountNonExpired) {
            instance.accountNonExpired = accountNonExpired;
        }

        public void setAccountNonLocked(final boolean accountNonLocked) {
            instance.accountNonLocked = accountNonLocked;
        }

        /**
         * Sets authorities.
         * @param authorities The authorities to set.
         */
        public void setAuthorities(final Collection<GrantedAuthority> authorities) {
            mutableAuthorities = new ArrayList<GrantedAuthority>();
            mutableAuthorities.addAll(authorities);
        }
        
        /**
         * Sets authorization groups.
         * @param authGroups The autorization groups to set.
         */
        public void setAuthGroups(final Collection<String> authGroups) {
            mutableAuthGroups = new ArrayList<String>();
            mutableAuthGroups.addAll(authGroups);
        }

        public void setCredentialsNonExpired(final boolean credentialsNonExpired) {
            instance.credentialsNonExpired = credentialsNonExpired;
        }

        public void setDn(final String dn) {
            instance.dn = dn;
        }

        public void setDn(final Name dn) {
            instance.dn = dn.toString();
        }

        public void setEnabled(final boolean enabled) {
            instance.enabled = enabled;
        }

        public void setPassword(final String password) {
            instance.password = password;
        }

        public void setUsername(final String username) {
            instance.username = username;
        }

        public void setTimeBeforeExpiration(final int timeBeforeExpiration) {
            instance.timeBeforeExpiration = timeBeforeExpiration;
        }

        public void setGraceLoginsRemaining(final int graceLoginsRemaining) {
            instance.graceLoginsRemaining = graceLoginsRemaining;
        }
    }

}
