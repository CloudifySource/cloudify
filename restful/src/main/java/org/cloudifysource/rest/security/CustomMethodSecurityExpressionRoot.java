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

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.core.Authentication;

/**
 * Extended expression root object which contains extra method-specific
 * functionality.
 * 
 * @author Noak
 * @since 2.3.1
 */
class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot {
	private PermissionEvaluator permissionEvaluator;
	private Object filterObject;
	private Object returnObject;
	public final String read = "read";
	public final String write = "write";
	public final String create = "create";
	public final String delete = "delete";
	public final String admin = "administration";

	CustomMethodSecurityExpressionRoot(Authentication a) {
		super(a);
	}

	public boolean hasPermission(Object target, Object permission) {
		if (target == null) {
			StringBuilder authGroups = new StringBuilder();
			if (authentication instanceof CustomAuthenticationToken) {
				for (String authGroup : ((CustomAuthenticationToken) authentication).getAuthGroups()) {
					if (authGroups.length() > 0) {
						authGroups.append(", ");
					}
					authGroups.append(authGroup);
				}
			}

			target = authGroups.toString();
		}
		return permissionEvaluator.hasPermission(authentication, target, permission);
	}

	public boolean hasPermission(Object targetId, String targetType, Object permission) {
		return permissionEvaluator.hasPermission(authentication, (Serializable) targetId, targetType, permission);
	}

	public void setFilterObject(Object filterObject) {
		this.filterObject = filterObject;
	}

	public Object getFilterObject() {
		return filterObject;
	}

	public void setReturnObject(Object returnObject) {
		this.returnObject = returnObject;
	}

	public Object getReturnObject() {
		return returnObject;
	}

	public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
		this.permissionEvaluator = permissionEvaluator;
	}

}
