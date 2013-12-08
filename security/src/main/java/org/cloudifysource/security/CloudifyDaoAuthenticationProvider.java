/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.security;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

/**
 * An {@link AuthenticationProvider} implementation that retrieves user details
 * from a {@link CloudifyUserDetailsService} to supports authorization groups on
 * top of authorities (roles).
 * 
 * @author noak
 * @since 2.7.0
 */
public class CloudifyDaoAuthenticationProvider implements AuthenticationProvider {

	private final Logger logger = java.util.logging.Logger.getLogger(CloudifyDaoAuthenticationProvider.class.getName());

	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

	private CloudifyUserDetailsService cloudifyUserDetailsService;


	/**
	 * Verifies the user details service is set.
	 * 
	 * @throws Exception
	 *             indicates the user details service was not set (probably not
	 *             configured probably in the xml configuration file)
	 */
	protected void doAfterPropertiesSet() throws Exception {
		Assert.notNull(this.cloudifyUserDetailsService, "A UserDetailsService must be set");
	}
	
	public CloudifyUserDetailsService getCloudifyUserDetailsService() {
		return cloudifyUserDetailsService;
	}


	public void setCloudifyUserDetailsService(final CloudifyUserDetailsService cloudifyUserDetailsService) {
		this.cloudifyUserDetailsService = cloudifyUserDetailsService;
	}
	

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication, messages.getMessage(
				"AbstractUserDetailsAuthenticationProvider.onlySupports",
				"Only UsernamePasswordAuthenticationToken is supported"));

		logger.finest("CloudifyDaoAuthenticationProvider: authenticate");
		final UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;
		final CloudifyUserDetails user;

		// Determine username
		final String username = userToken.getName();
		final String password = (String) authentication.getCredentials();

		if (StringUtils.isBlank(username)) {
			throw new IllegalArgumentException("Empty username not allowed");
		}
		Assert.notNull(password, "Null password was supplied in authentication token");
		logger.fine("Processing authentication request for user: " + username);

		// Get the Cloudify user details from the user details service
		try {
			user = retrieveUser(username);
		} catch (final UsernameNotFoundException e) {
			logger.warning("User '" + username + "' not found");
			throw e;
		}

		// authenticate
		runAuthenticationChecks(user);

		// create a successful and full authentication token
		return createSuccessfulAuthentication(userToken, user);
	}
	

	private CloudifyUserDetails retrieveUser(final String username) throws AuthenticationException {

		CloudifyUserDetails loadedUser;
		try {
			loadedUser = this.getCloudifyUserDetailsService().loadUserByUsername(username);
		} catch (final UsernameNotFoundException e) {
			throw e;
		} catch (final Exception repositoryProblem) {
			throw new AuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
		}

		if (loadedUser == null) {
			throw new AuthenticationServiceException(
					"CloudifyUserDetailsService returned null, which is an interface contract violation");
		}
		return loadedUser;
	}
	

	/**
	 * Creates the final <tt>Authentication</tt> object which will be returned
	 * from the <tt>authenticate</tt> method.
	 * 
	 * @param authentication
	 *            the original authentication request token
	 * @param user
	 *            the <tt>UserDetails</tt> instance returned by the configured
	 *            <tt>UserDetailsContextMapper</tt>.
	 * @return the Authentication object for the fully authenticated user.
	 */
	protected Authentication createSuccessfulAuthentication(final UsernamePasswordAuthenticationToken authentication,
			final CloudifyUserDetails user) {

		logger.finest("starting createSuccessfulAuthentication");

		final CustomAuthenticationToken customAuthToken = new CustomAuthenticationToken(user,
				authentication.getCredentials(), user.getAuthorities(), user.getAuthGroups());
		customAuthToken.setDetails(authentication.getDetails());

		return customAuthToken;
	}
	

	private void runAuthenticationChecks(final CloudifyUserDetails user) {
		if (!user.isAccountNonLocked()) {
			logger.warning("User account is locked");

			throw new LockedException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked",
					"User account is locked"));
		}

		if (!user.isEnabled()) {
			logger.warning("User account is disabled");

			throw new DisabledException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled",
					"User is disabled"));
		}

		if (!user.isAccountNonExpired()) {
			logger.warning("User account is expired");

			throw new AccountExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.expired",
					"User account has expired"));
		}
		
		if (!user.isCredentialsNonExpired()) {
			logger.warning("User account credentials have expired");

			throw new CredentialsExpiredException(messages.getMessage(
					"AbstractUserDetailsAuthenticationProvider.credentialsExpired", "User credentials have expired"));
		}
	}
	
	
	@Override
	public boolean supports(final Class<?> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}
