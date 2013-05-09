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

import java.io.Serializable;
import java.util.logging.Logger;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * A permission evaluator that always denies access. Used as a default if no other evaluator is found. 
 *
 * @author noak
 * @since 2.3.0
 */
public class CustomDenyAllPermissionEvaluator implements PermissionEvaluator {

	private Logger logger = java.util.logging.Logger.getLogger(CustomDenyAllPermissionEvaluator.class.getName());

	/**
	 * @param authentication The authentication object of the current user
	 * @param target The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return false always
	 */
	public boolean hasPermission(final Authentication authentication, final Object target, final Object permission) {
        logger.fine("Denying user " + authentication.getName() + " permission '" + permission + "' on object " 
        		+ target);
        return false;
    }

	/**
	 * @param authentication The authentication object of the current user
	 * @param targetId The A unique identifier of the target object the user is attempting to access
	 * @param targetType The type of the target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return false always
	 */
	public boolean hasPermission(final Authentication authentication, final Serializable targetId, 
			final String targetType, final Object permission) {
        logger.fine("Denying user " + authentication.getName() + " permission '" + permission + "' on object with Id '"
                        + targetId);
        return false;
    }

}
