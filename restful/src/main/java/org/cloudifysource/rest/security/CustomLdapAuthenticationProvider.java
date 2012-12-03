package org.cloudifysource.rest.security;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.ppolicy.PasswordPolicyException;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class CustomLdapAuthenticationProvider implements AuthenticationProvider {

    private static final Log logger = LogFactory.getLog(LdapAuthenticationProvider.class);

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
     * @param useAuthenticationRequestCredentials
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

        final UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;

        String username = userToken.getName();
        String password = (String) authentication.getCredentials();

        if (logger.isDebugEnabled()) {
            logger.debug("Processing authentication request for user: " + username);
        }

        if (!StringUtils.hasLength(username)) {
            throw new BadCredentialsException(messages.getMessage("LdapAuthenticationProvider.emptyUsername",
                    "Empty Username"));
        }

        Assert.notNull(password, "Null password was supplied in authentication token");

        try {
            DirContextOperations userData = getAuthenticator().authenticate(authentication);

            Collection<GrantedAuthority> extraAuthorities = loadUserAuthorities(userData, username, password);
            
            Collection<String> userAuthGroups = loadUserAuthGroups(userData, username, password);

            ExtendedLdapUserDetailsImpl extendedUserDetails = 
            		userDetailsContextMapper.mapUserFromContext(userData, username, extraAuthorities, userAuthGroups);

            return createSuccessfulAuthentication(userToken, extendedUserDetails);
            
        } catch (PasswordPolicyException ppe) {
            // The only reason a ppolicy exception can occur during a bind is that the account is locked.
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
    protected Collection<GrantedAuthority> loadUserAuthorities(final DirContextOperations user, final String username, 
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
        Object password = useAuthenticationRequestCredentials ? authentication.getCredentials() : user.getPassword();

        /*UsernamePasswordAuthenticationToken result = 
        		new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
        result.setDetails(authentication.getDetails());*/
        
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
    public boolean supports(final Class<? extends Object> authentication) {
        if (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)) {
        	return true;
        }
        return false;
    }

}