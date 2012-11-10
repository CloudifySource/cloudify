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
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.cloudifysource.rest.util.RestUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * 
 * @author noak
 * @since 2.3.1
 *
 */
public class ExtendedMethodSecurityExpressionHandler extends
		DefaultMethodSecurityExpressionHandler implements
		MethodSecurityExpressionHandler {

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	// private PermissionEvaluator permissionEvaluator = new
	// DenyAllPermissionEvaluator();
	private PermissionEvaluator permissionEvaluator = new CustomDenyAllPermissionEvaluator();
	private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
	private ExpressionParser expressionParser = new SpelExpressionParser();
	private RoleHierarchy roleHierarchy;

	/**
	 * Uses a {@link CustomMethodSecurityEvaluationContext} as the
	 * <tt>EvaluationContext</tt> implementation and configures it with a
	 * {@link CustomMethodSecurityExpressionRoot} instance as the expression
	 * root object.
	 */
	@Override
	public EvaluationContext createEvaluationContext(Authentication auth,
			MethodInvocation mi) {
		CustomMethodSecurityEvaluationContext ctx = new CustomMethodSecurityEvaluationContext(
				auth, mi, parameterNameDiscoverer);
		CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(
				auth);
		root.setTrustResolver(trustResolver);
		root.setPermissionEvaluator(permissionEvaluator);
		root.setRoleHierarchy(roleHierarchy);
		ctx.setRootObject(root);

		return ctx;
	}
	
	public ExpressionParser getExpressionParser() {
        return expressionParser;
    }

    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
        this.trustResolver = trustResolver;
    }

    public void setReturnObject(Object returnObject, EvaluationContext ctx) {
        ((CustomMethodSecurityExpressionRoot)ctx.getRootObject().getValue()).setReturnObject(returnObject);
    }

    public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

	@Override
	@SuppressWarnings("unchecked")
	public Object filter(Object filterTarget, Expression filterExpression,
			EvaluationContext ctx) {
		CustomMethodSecurityExpressionRoot rootObject = (CustomMethodSecurityExpressionRoot) ctx
				.getRootObject().getValue();

		if (logger.isDebugEnabled()) {
			logger.debug("Filtering with expression: "
					+ filterExpression.getExpressionString());
		}

		if (filterTarget instanceof Collection ||
				filterTarget.getClass().isArray()) {
			return super.filter(filterTarget, filterExpression, ctx);
		}

		if (filterTarget instanceof Map<?, ?>) {
			Map<String, Object> restReturnObj = (Map<String, Object>) filterTarget;
			Map<Object, String> retainMap = new HashMap<Object, String>();

			if (restReturnObj.get(RestUtils.STATUS_KEY).equals(
					RestUtils.SUCCESS)) {
				Object appsMapObject = restReturnObj
						.get(RestUtils.RESPONSE_KEY);
				if (appsMapObject != null && appsMapObject instanceof Map) {
					Map<String, String> appsMap = (Map<String, String>) appsMapObject;

					if (logger.isDebugEnabled()) {
						logger.debug("Filtering map with " + appsMap.size()
								+ " elements");
					}

					for (Map.Entry<String, String> appEntry : appsMap
							.entrySet()) {
						String filterObject = appEntry.getValue();
						rootObject.setFilterObject(filterObject);

						if (ExpressionUtils.evaluateAsBoolean(filterExpression,
								ctx)) {
							retainMap.put(appEntry.getKey(),
									appEntry.getValue());
						}
					}

				}
				return RestUtils.successStatus(retainMap);
			}
		}

		throw new IllegalArgumentException(
				"Filter target must be a collection or array type, but was "
						+ filterTarget);
	}
}