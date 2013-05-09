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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * LDAP authorities populator, replaces Spring's DafaultLdapAuthoritiesPopulator.
 * The strategy here is to load authorities from predefined user attribute(s) "roleAttributes".
 * The role values are set as is. As opposed to the default populator the roles values are not
 * manipulated (no prefixing or case-changing is done) and they are not related to group membership. 
 * 
 * @author noak
 * @since 2.3.0
 */
public class CustomLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
	private static final Log logger = LogFactory.getLog(CustomLdapAuthoritiesPopulator.class);

    /**
     * Attributes of the User's LDAP Object that contain role name information.
     */
   private String[] roleAttributes;

    //~ Constructors ===================================================================================================

    /**
     * Constructor.
     *
     * @param roleAttributes The role attributes.
     */
    public CustomLdapAuthoritiesPopulator(final String[] roleAttributes) {
    	this.roleAttributes = roleAttributes;
    }


    /**
     * Obtains the authorities for the user who's directory entry is represented by
     * the supplied LdapUserDetails object.
     *
     * @param user the user who's authorities are required
     * @param username the user name
     * @return the set of roles granted to the user.
     */
    public final Collection<GrantedAuthority> getGrantedAuthorities(final DirContextOperations user, 
    		final String username) {
    	
    	String userDn = user.getNameInNamespace();
    	 if (roleAttributes == null || roleAttributes.length == 0) {
             return Collections.emptySet();
         }

         Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

         if (logger.isDebugEnabled()) {
             logger.debug("Setting roles for user '" + username + "', DN = " + "'" + userDn + " using attributes: "
            		 +  Arrays.toString(roleAttributes));
         }
         
         for (String roleAttribute : roleAttributes) {
             String[] rolesFromAttribute = user.getStringAttributes(roleAttribute);
             if (rolesFromAttribute == null) {
                 if (logger.isDebugEnabled()) {
                    logger.debug("Couldn't read role attribute '" + roleAttribute + "' for user " + userDn);
                 }
                 continue;
             }

             for (String roleFromAttribute : rolesFromAttribute) {
                 GrantedAuthority authority = new GrantedAuthorityImpl(roleFromAttribute);
                 authorities.add(authority);
             }
         }

         return authorities;
    }

}
