/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import java.util.Collection;

import org.springframework.security.core.Authentication;

/**
 * Extends the {@link Authentication} interface, adding the AuthGroups to it.
 * @author noak
 * @since 2.3.0 *
 */
public interface ExtendedAuthentication extends Authentication {
	
    /**
     * Set by an <code>AuthenticationManager</code> to indicate the authorization groups that the principal belongs to.
     * Note that classes should not rely on this value as being valid unless it has been set by a trusted
     * <code>AuthenticationManager</code>.
     * <p>
     * Implementations should ensure that modifications to the returned collection
     * array do not affect the state of the Authentication object, or use an unmodifiable instance.
     * </p>
     *
     * @return the authorization groups the principal belongs to, or an empty collection if the token has not 
     * been authenticated.
     * Never null.
     */
    Collection<String> getAuthGroups();

}
