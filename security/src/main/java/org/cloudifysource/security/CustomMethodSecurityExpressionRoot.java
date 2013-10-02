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

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Extended expression root object which contains extra method-specific
 * functionality.
 * 
 * @author Noak
 * @since 2.3.1
 */
class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
	
	private PermissionEvaluator permissionEvaluator;
	private Object filterObject;
	private Object returnObject;
	private Object target;
	
	CustomMethodSecurityExpressionRoot(final Authentication a) {
		super(a);
	}

	public boolean hasPermission(final Object target, final Object permission) {
		Object effectiveTarget = target;
		if (effectiveTarget == null) {
			StringBuilder authGroups = new StringBuilder();
			if (authentication instanceof CustomAuthenticationToken) {
				for (String authGroup : ((CustomAuthenticationToken) authentication).getAuthGroups()) {
					if (authGroups.length() > 0) {
						authGroups.append(", ");
					}
					authGroups.append(authGroup);
				}
			} else {
				for (GrantedAuthority authority : (authentication).getAuthorities()) {
					if (authGroups.length() > 0) {
						authGroups.append(", ");
					}
					authGroups.append(authority.getAuthority());
				}
			}

			effectiveTarget = authGroups.toString();
		}
		return permissionEvaluator.hasPermission(authentication, effectiveTarget, permission);
	}

	public boolean hasPermission(final Object targetId, final String targetType, final Object permission) {
		return permissionEvaluator.hasPermission(authentication, (Serializable) targetId, targetType, permission);
	}

	public void setFilterObject(final Object filterObject) {
		this.filterObject = filterObject;
	}

	public Object getFilterObject() {
		return filterObject;
	}

	public void setReturnObject(final Object returnObject) {
		this.returnObject = returnObject;
	}

	public Object getReturnObject() {
		return returnObject;
	}

	public void setPermissionEvaluator(final PermissionEvaluator permissionEvaluator) {
		this.permissionEvaluator = permissionEvaluator;
	}
	
    /**
     * Sets the "this" property for use in expressions. Typically this will be the "this" property of
     * the {@code JoinPoint} representing the method invocation which is being protected.
     *
     * @param target the target object on which the method in is being invoked.
     */
    void setThis(Object target) {
        this.target = target;
    }

    public Object getThis() {
        return target;
    }
}
