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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * A permission evaluator that always allows access. Used for a non-secured configuration in 
 * combination with the "nonsecure" beans profile.
 * 
 * @author noak
 * @since 2.3.0
 */
public class CustomApproveAllPermissionEvaluator implements PermissionEvaluator {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * @return true always
	 */
	@Override
	public boolean hasPermission(final Authentication authentication, final Object target, final Object permission) {
		logger.warn("Grant user " + authentication.getName() + " permission '" + permission + "' on object " + target);
		return true;
	}

	/**
	 * @return true always
	 */
	@Override
	public boolean hasPermission(final Authentication authentication, final Serializable targetId, final String targetType,
			Object permission) {
		logger.warn("Grant user " + authentication.getName() + " permission '" + permission + "' on object with Id '"
				+ targetId);
		return true;
	}

}