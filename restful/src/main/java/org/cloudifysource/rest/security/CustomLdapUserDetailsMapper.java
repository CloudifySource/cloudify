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
package org.cloudifysource.rest.security;

import java.util.Collection;
import java.util.logging.Logger;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.util.Assert;

/**
 * A custom implementation for Spring's UserDetailsContextMapper.
 * The context mapper used by the LDAP authentication provider to create an LDAP user object with both groups and roles.
 *
 * @author Noak
 * @since 2.3.0
 * 
 */
public class CustomLdapUserDetailsMapper {
	//~ Instance fields ================================================================================================

    private Logger logger = java.util.logging.Logger.getLogger(CustomLdapUserDetailsMapper.class.getName());
    private String passwordAttributeName = "userPassword";
    private String[] roleAttributes = null;
    private boolean convertToUpperCase = true;

    /**
     * 
     * @param ctx User object
     * @param username the user name
     * @param authorities A collection of authorities
     * @param authGroups A collection of authentication groups
     * @return UserDetails
     */
    public ExtendedLdapUserDetailsImpl mapUserFromContext(final DirContextOperations ctx, final String username, 
    		final Collection<GrantedAuthority> authorities, final Collection<String> authGroups) {
        String dn = ctx.getNameInNamespace();

        logger.finest("CustomLdapUserDetailsMapper: mapUserFromContext");
        logger.fine("Mapping user details from context with DN: " + dn);

        ExtendedLdapUserDetailsImpl.ExtendedEssence essence = new ExtendedLdapUserDetailsImpl.ExtendedEssence();
        essence.setDn(dn);

        Object passwordValue = ctx.getObjectAttribute(passwordAttributeName);

        if (passwordValue != null) {
            essence.setPassword(mapPassword(passwordValue));
        }

        essence.setUsername(username);

        // Map the roles
        for (int i = 0; (roleAttributes != null) && (i < roleAttributes.length); i++) {
            String[] rolesForAttribute = ctx.getStringAttributes(roleAttributes[i]);

            if (rolesForAttribute == null) {
                logger.fine("Couldn't read role attribute '" + roleAttributes[i] + "' for user " + dn);
                continue;
            }

            for (String roleForAttribute : rolesForAttribute) {
                GrantedAuthority authority = createAuthority(roleForAttribute);

                if (authority != null) {
                    essence.addAuthority(authority);
                }
            }
        }

        // Add the supplied authorities

        for (GrantedAuthority authority : authorities) {
            essence.addAuthority(authority);
        }
        
        // Add the supplied authorization groups
        for (String authGroup : authGroups) {
        	essence.addAuthGroup(authGroup);
        }

        // Check for PPolicy data

        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ctx.
        		getObjectAttribute(PasswordPolicyControl.OID);

        if (ppolicy != null) {
            essence.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
            essence.setGraceLoginsRemaining(ppolicy.getGraceLoginsRemaining());
        }

        return essence.createUserDetails();

    }

    /**
     * Not implemented.
     * @param user .
     * @param ctx .
     */
    public void mapUserToContext(final UserDetails user, final DirContextAdapter ctx) {
        throw new UnsupportedOperationException("LdapUserDetailsMapper only supports reading from a context. Please" 
        		+ "use a subclass if mapUserToContext() is required.");
    }

    /**
     * Extension point to allow customized creation of the user's password from
     * the attribute stored in the directory.
     *
     * @param passwordValue the value of the password attribute
     * @return a String representation of the password.
     */
    protected String mapPassword(final Object passwordValue) {

    	String passwordString;
        if (passwordValue instanceof String) {
        	passwordString = (String) passwordValue;
        } else {
        	// Assume it's binary
        	passwordString = new String((byte[]) passwordValue);
        }

        return passwordString;

    }

    /**
     * Creates a GrantedAuthority from a role attribute. Override to customize
     * authority object creation.
     * <p>
     * The default implementation converts string attributes to roles, making use of the <tt>rolePrefix</tt>
     * and <tt>convertToUpperCase</tt> properties. Non-String attributes are ignored.
     * </p>
     *
     * @param role the attribute returned from
     * @return the authority to be added to the list of authorities for the user, or null
     * if this attribute should be ignored.
     */
    protected GrantedAuthority createAuthority(final Object role) {
    	String roleString;
    	
        if (role instanceof String) {
        	roleString = (String) role;
            if (convertToUpperCase) {
            	roleString = roleString.toUpperCase();
            }
            return new GrantedAuthorityImpl(roleString);
        }
        return null;
    }

    /**
     * Determines whether role field values will be converted to upper case when loaded.
     * The default is true.
     *
     * @param convertToUpperCase true if the roles should be converted to upper case.
     */
    public void setConvertToUpperCase(final boolean convertToUpperCase) {
        this.convertToUpperCase = convertToUpperCase;
    }

    /**
     * The name of the attribute which contains the user's password.
     * Defaults to "userPassword".
     *
     * @param passwordAttributeName the name of the attribute
     */
    public void setPasswordAttributeName(final String passwordAttributeName) {
        this.passwordAttributeName = passwordAttributeName;
    }

    /**
     * The names of any attributes in the user's  entry which represent application
     * roles. These will be converted to <tt>GrantedAuthority</tt>s and added to the
     * list in the returned LdapUserDetails object. The attribute values must be Strings by default.
     *
     * @param roleAttributes the names of the role attributes.
     */
    public void setRoleAttributes(final String[] roleAttributes) {
        Assert.notNull(roleAttributes, "roleAttributes array cannot be null");
        this.roleAttributes = roleAttributes;
    }

}
