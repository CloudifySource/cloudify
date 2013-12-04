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

import java.util.Collection;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Extending Spring's {@link UserDetails} interface to support authorization groups as well.
 * 
 * @author noak
 * @since 2.7
 */
public interface CloudifyUserDetails extends UserDetails {

    /**
     * Gets the permitted security roles for the active user as a collection of Strings.
     * @return A collection of roles as string values.
     */
    Collection<String> getRoles();


    /**
     * Gets the registered authorization groups for the active user.
     * @return A collection of authorization groups as string values.
     */
    Collection<String> getAuthGroups();
}
