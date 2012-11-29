package org.cloudifysource.rest.security;

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
 *
 */
public class CustomLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
	private static final Log logger = LogFactory.getLog(CustomLdapAuthoritiesPopulator.class);

    /**
     * Attributes of the User's LDAP Object that contain role name information.
     */
   private String[] roleAttributes = null;

    //~ Constructors ===================================================================================================

    /**
     * Constructor.
     *
     * @param contextSource supplies the contexts used to search for user roles.
     * @param groupSearchBase          if this is an empty string the search will be performed from the root DN of the
     *                                 context factory.
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
         
         for (int i = 0; i < roleAttributes.length; i++) {
             String[] rolesFromAttribute = user.getStringAttributes(roleAttributes[i]);
             if (rolesFromAttribute == null) {
                 logger.debug("Couldn't read role attribute '" + roleAttributes[i] + "' for user " + userDn);
                 continue;
             }

             for (int j = 0; j < rolesFromAttribute.length; j++) {
                 GrantedAuthority authority = new GrantedAuthorityImpl(rolesFromAttribute[j]);
                 authorities.add(authority);
             }
         }

         return authorities;
    }

}
