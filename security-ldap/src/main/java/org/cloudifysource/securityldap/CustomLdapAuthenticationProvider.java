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

import java.util.Collection;
import java.util.logging.Logger;

import org.cloudifysource.security.CustomAuthenticationToken;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.ppolicy.PasswordPolicyException;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This is a custom LdapAuthenticationProvider that supports authorization groups on top of authorities (roles).
 * @author noak
 * @since 2.3.0
 *
 */
public class CustomLdapAuthenticationProvider implements AuthenticationProvider {

    private Logger logger = java.util.logging.Logger.getLogger(CustomLdapAuthenticationProvider.class.getName());

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    private LdapAuthenticator authenticator;
    private LdapAuthoritiesPopulator authoritiesPopulator;
    private LdapAuthGroupsPopulator authGroupsPopulator;
    private CustomLdapUserDetailsMapper userDetailsContextMapper = new CustomLdapUserDetailsMapper();
    private boolean useAuthenticationRequestCredentials = true;
    private boolean hideUserNotFoundExceptions = true;

    /**
     * Create an instance with the supplied authenticator and authorities populator implementations.
     *
     * @param authenticator the authentication strategy (bind, password comparison, etc)
     *          to be used by this provider for authenticating users.
     * @param authoritiesPopulator the strategy for obtaining the authorities for a given user after they've been
     *          authenticated.
     */
    public CustomLdapAuthenticationProvider(final LdapAuthenticator authenticator,
    		final LdapAuthoritiesPopulator authoritiesPopulator,
    		final LdapAuthGroupsPopulator authGroupsPopulator) {
    	logger.finest("CustomLdapAuthenticationProvider : constructor");
        this.setAuthenticator(authenticator);
        this.setAuthoritiesPopulator(authoritiesPopulator);
        this.setAuthGroupsPopulator(authGroupsPopulator);
    }

    private void setAuthenticator(final LdapAuthenticator authenticator) {
        Assert.notNull(authenticator, "An LdapAuthenticator must be supplied");
        this.authenticator = authenticator;
    }

    private LdapAuthenticator getAuthenticator() {
        return authenticator;
    }

    private void setAuthoritiesPopulator(final LdapAuthoritiesPopulator authoritiesPopulator) {
        Assert.notNull(authoritiesPopulator, "An LdapAuthoritiesPopulator must be supplied");
        this.authoritiesPopulator = authoritiesPopulator;
    }

    protected LdapAuthoritiesPopulator getAuthoritiesPopulator() {
        return authoritiesPopulator;
    }
    
    private void setAuthGroupsPopulator(final LdapAuthGroupsPopulator authGroupsPopulator) {
        Assert.notNull(authGroupsPopulator, "An LdapAuthGroupsPopulator must be supplied");
        this.authGroupsPopulator = authGroupsPopulator;
    }

    protected LdapAuthGroupsPopulator getAuthGroupsPopulator() {
        return authGroupsPopulator;
    }

    /**
     * Allows a custom strategy to be used for creating the <tt>UserDetails</tt> which will be stored as the principal
     * in the <tt>Authentication</tt> returned by the
     * {@link #createSuccessfulAuthentication(UsernamePasswordAuthenticationToken, UserDetails)} method.
     *
     * @param userDetailsContextMapper the strategy instance. If not set, defaults to a simple
     * <tt>LdapUserDetailsMapper</tt>.
     */
    public void setUserDetailsContextMapper(final CustomLdapUserDetailsMapper userDetailsContextMapper) {
        Assert.notNull(userDetailsContextMapper, "UserDetailsContextMapper must not be null");
        this.userDetailsContextMapper = userDetailsContextMapper;
    }

    /**
     * Provides access to the injected <tt>UserDetailsContextMapper</tt> strategy for use by subclasses.
     * @return CustomLdapUserDetailsMapper.
     */
    protected CustomLdapUserDetailsMapper getUserDetailsContextMapper() {
        return userDetailsContextMapper;
    }

    public void setHideUserNotFoundExceptions(final boolean hideUserNotFoundExceptions) {
        this.hideUserNotFoundExceptions = hideUserNotFoundExceptions;
    }

    /**
     * Determines whether the supplied password will be used as the credentials in the successful authentication
     * token. If set to false, then the password will be obtained from the UserDetails object
     * created by the configured <tt>UserDetailsContextMapper</tt>.
     * Often it will not be possible to read the password from the directory, so defaults to true.
     *
     * @param useAuthenticationRequestCredentials true/false, as describes above
     */
    public void setUseAuthenticationRequestCredentials(final boolean useAuthenticationRequestCredentials) {
        this.useAuthenticationRequestCredentials = useAuthenticationRequestCredentials;
    }

    public void setMessageSource(final MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * This is the main method of this class, calling authentication, authorization and user details mapping.
     * @param authentication object to populate
     * @return Populated authentication object
     * @throws AuthenticationException
     */
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
            messages.getMessage("AbstractUserDetailsAuthenticationProvider.onlySupports",
                "Only UsernamePasswordAuthenticationToken is supported"));
        
        logger.finest("CustomLdapAuthenticationProvider: authenticate");

        final UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;

        String username = userToken.getName();
        String password = (String) authentication.getCredentials();

        logger.fine("Processing authentication request for user: " + username);

        if (!StringUtils.hasLength(username)) {
            throw new BadCredentialsException(messages.getMessage("LdapAuthenticationProvider.emptyUsername",
                    "Empty Username"));
        }

        Assert.notNull(password, "Null password was supplied in authentication token");

        try {
            DirContextOperations userData = getAuthenticator().authenticate(authentication);

            Collection<? extends GrantedAuthority> extraAuthorities = loadUserAuthorities(userData, username, password);
            
            Collection<String> userAuthGroups = loadUserAuthGroups(userData, username, password);

            ExtendedLdapUserDetailsImpl extendedUserDetails = 
            		userDetailsContextMapper.mapUserFromContext(userData, username, extraAuthorities, userAuthGroups);

            return createSuccessfulAuthentication(userToken, extendedUserDetails);
            
        } catch (PasswordPolicyException ppe) {
            // The only reason a policy exception can occur during a bind is that the account is locked.
            throw new LockedException(messages.getMessage(ppe.getStatus().getErrorCode(),
                    ppe.getStatus().getDefaultMessage()));
        } catch (UsernameNotFoundException notFound) {
            if (hideUserNotFoundExceptions) {
                throw new BadCredentialsException(messages.getMessage(
                        "LdapAuthenticationProvider.badCredentials", "Bad credentials"));
            } else {
                throw notFound;
            }
        } catch (NamingException ldapAccessFailure) {
            throw new AuthenticationServiceException(ldapAccessFailure.getMessage(), ldapAccessFailure);
        }
    }

    /**
     * loads the user's roles (authorities).
     * @param user .
     * @param username .
     * @param password .
     * @return Collections of {@link GrantedAuthority} objects
     */
    protected Collection<? extends GrantedAuthority> loadUserAuthorities(final DirContextOperations user, final String username, 
    		final String password) {
        return getAuthoritiesPopulator().getGrantedAuthorities(user, username);
    }
    
    /**
     * loads the user's authorization groups.
     * @param user .
     * @param username .
     * @param password .
     * @return Collection of authorization groups names.
     */
    protected Collection<String> loadUserAuthGroups(final DirContextOperations user, final String username, 
    		final String password) {
        return getAuthGroupsPopulator().getAuthGroups(user, username);
    }

    /**
     * Creates the final <tt>Authentication</tt> object which will be returned from the <tt>authenticate</tt> method.
     *
     * @param authentication the original authentication request token
     * @param user the <tt>UserDetails</tt> instance returned by the configured <tt>UserDetailsContextMapper</tt>.
     * @return the Authentication object for the fully authenticated user.
     */
    protected Authentication createSuccessfulAuthentication(final UsernamePasswordAuthenticationToken authentication,
            final ExtendedLdapUserDetailsImpl user) {
    	
    	logger.finest("CustomLdapAuthenticationProvider : createSuccessfulAuthentication");
        Object password = useAuthenticationRequestCredentials ? authentication.getCredentials() : user.getPassword();

        CustomAuthenticationToken customAuthToken = new CustomAuthenticationToken(user, password, 
        		user.getAuthorities(), user.getAuthGroups());
        customAuthToken.setDetails(authentication.getDetails());

        return customAuthToken;
    }

    /**
     * Checks if the authentication object passed is supported.
     * @param authentication The authentication object to check.
     * @return true - supported, false - otherwise.
     */
    public boolean supports(final Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}