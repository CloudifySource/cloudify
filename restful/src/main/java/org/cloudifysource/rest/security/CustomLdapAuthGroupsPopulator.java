package org.cloudifysource.rest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.util.Assert;

/**
 * Populates the user's authorization groups based on group membership.
 * @author noak
 * @since 2.3.0
 *
 */
public class CustomLdapAuthGroupsPopulator implements LdapAuthGroupsPopulator {

    private static final Log logger = LogFactory.getLog(CustomLdapAuthGroupsPopulator.class);

    private SpringSecurityLdapTemplate ldapTemplate;

    /**
     * Controls used to determine whether group searches should be performed over the full sub-tree from the
     * base DN. Modified by searchSubTree property
     */
    private SearchControls searchControls = new SearchControls();

    /**
     * The ID of the attribute which contains the authorization group name.
     */
    private String groupNameAttribute = "cn";

    /**
     * The base DN from which the search for group membership should be performed.
     */
    private String groupSearchBase;

    /**
     * The pattern to be used for the user search. {0} is the user's DN
     */
    private String groupSearchFilter = "(member={0})";

    /**
     * Attributes of the User's LDAP Object that contain role name information.
     */

//  private String[] userRoleAttributes = null;


    /**
     * Constructor.
     *
     * @param contextSource supplies the contexts used to search for group membership.
     * @param groupSearchBase          if this is an empty string the search will be performed from the root DN of the
     *                                 context factory.
     */
    public CustomLdapAuthGroupsPopulator(final ContextSource contextSource, final String groupSearchBase) {
        Assert.notNull(contextSource, "contextSource must not be null");
        ldapTemplate = new SpringSecurityLdapTemplate(contextSource);
        ldapTemplate.setSearchControls(searchControls);
        setGroupSearchBase(groupSearchBase);
    }


    /**
     * Obtains the authorization groups for the user.
     *
     * @param user the user who's authorities are required
     * @param username the user name
     * @return the authorization groups of the given user.
     */
    public final Collection<String> getAuthGroups(final DirContextOperations user, final String username) {
        String userDn = user.getNameInNamespace();

        if (logger.isDebugEnabled()) {
            logger.debug("Getting authorization groups for user " + userDn);
        }

        Set<String> authGroups = getAuthGroups(userDn, username);

        List<String> result = new ArrayList<String>(authGroups.size());
        result.addAll(authGroups);

        return result;
    }

    /**
     * Obtains the authorization groups for the user.
     * 
     * @param userDn The user DN
     * @param username The user name
     * @return Authorization groups
     */
    public Set<String> getAuthGroups(final String userDn, final String username) {
        if (getGroupSearchBase() == null) {
            return Collections.emptySet();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Searching for authorization groups for user '" + username + "', DN = " + "'" + userDn 
            		+ "', with filter " + groupSearchFilter + " in search base '" + getGroupSearchBase() + "'");
        }

        Set<String> authGroups = ldapTemplate.searchForSingleAttributeValues(getGroupSearchBase(), groupSearchFilter,
                new String[]{userDn, username}, groupNameAttribute);

        if (logger.isDebugEnabled()) {
            logger.debug("Authorization groups from search: " + authGroups);
        }

        return authGroups;
    }

    protected ContextSource getContextSource() {
        return ldapTemplate.getContextSource();
    }

    /**
     * Set the group search base (name to search under).
     *
     * @param groupSearchBase if this is an empty string the search will be performed from the root DN of the context
     *                        factory.
     */
    private void setGroupSearchBase(final String groupSearchBase) {
        Assert.notNull(groupSearchBase, "The groupSearchBase (name to search under), must not be null.");
        this.groupSearchBase = groupSearchBase;
        if (groupSearchBase.length() == 0) {
            logger.info("groupSearchBase is empty. Searches will be performed from the context source base");
        }
    }

    protected String getGroupSearchBase() {
        return groupSearchBase;
    }


    /**
     * Sets the attribute which contains the authorization group name.
     * @param groupNameAttribute The attribute which contains the authorization group name.
     */
    public void setGroupNameAttribute(final String groupNameAttribute) {
        Assert.notNull(groupNameAttribute, "groupNameAttribute must not be null");
        this.groupNameAttribute = groupNameAttribute;
    }

    /**
     * Sets the pattern to be used for the user search.
     * @param groupSearchFilter The pattern to be used for the user search.
     */
    public void setGroupSearchFilter(final String groupSearchFilter) {
        Assert.notNull(groupSearchFilter, "groupSearchFilter must not be null");
        this.groupSearchFilter = groupSearchFilter;
    }

    /**
     * If set to true, a subtree scope search will be performed. If false a single-level search is used.
     *
     * @param searchSubtree set to true to enable searching of the entire tree below the <tt>groupSearchBase</tt>.
     */
    public void setSearchSubtree(final boolean searchSubtree) {
        int searchScope = searchSubtree ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE;
        searchControls.setSearchScope(searchScope);
    }

    /**
     * Sets the corresponding property on the underlying template, avoiding specific issues with Active Directory.
     *
     *   @see LdapTemplate#setIgnoreNameNotFoundException(boolean)
     */
    /*public void setIgnorePartialResultException(final boolean ignore) {
        ldapTemplate.setIgnorePartialResultException(ignore);
    }*/

}
