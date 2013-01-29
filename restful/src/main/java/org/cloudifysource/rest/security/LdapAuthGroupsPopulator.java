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

import org.springframework.ldap.core.DirContextOperations;

/**
 * Obtains a list of authorization groups for an Ldap user.
 * <p>
 * Used by the <tt>CustomLdapAuthenticationProvider</tt> once a user has been
 * authenticated to create the final user details object.
 * </p>
 *
 * @author noak
 * @since 2.3.0
 */
public interface LdapAuthGroupsPopulator {
	
    /**
     * Get the list of authorization groups of the user.
     *
     * @param user the user who's authorities are required
     * @param username the user name.
     *
     * @return the authorization groups of the given user.
     *
     */
	Collection<String> getAuthGroups(final DirContextOperations user, final String username);

}
