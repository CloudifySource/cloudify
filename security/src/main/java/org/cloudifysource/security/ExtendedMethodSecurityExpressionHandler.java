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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * An extended MethodSecurityExpressionHandler.
 * @author noak
 * @since 2.3.1
 *
 */
public class ExtendedMethodSecurityExpressionHandler extends
		DefaultMethodSecurityExpressionHandler implements
		MethodSecurityExpressionHandler {

	private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
	
	// base class does not have a getter so we had to copy the same code here
	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	
	@Override
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }
	
	private Logger logger = java.util.logging.Logger.getLogger(ExtendedMethodSecurityExpressionHandler.class.getName());

	@PostConstruct
	public void overrideDefaults() {
		setPermissionEvaluator(new CustomDenyAllPermissionEvaluator());
	}
	
	/**
	 * Uses a {@link CustomMethodSecurityEvaluationContext} as the
	 * <tt>EvaluationContext</tt> implementation and configures it with a
	 * {@link CustomMethodSecurityExpressionRoot} instance as the expression
	 * root object.
	 * @param auth  The {@link Authentication} object of the current user
	 * @param mi The attempted method invocation
	 * @return EvaluationContext, containing the permission evaluator to be used
	 */
	@Override
	public StandardEvaluationContext createEvaluationContextInternal(final Authentication auth, final MethodInvocation mi) {
		return new CustomMethodSecurityEvaluationContext(auth, mi, parameterNameDiscoverer);
	}
	
	  /**
     * Creates the root object for expression evaluation.
     */
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
    	CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(authentication);
        root.setThis(invocation.getThis());
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(trustResolver);
        root.setRoleHierarchy(getRoleHierarchy());
        return root;
    }
	
    @Override
	public void setReturnObject(final Object returnObject, final EvaluationContext ctx) {
        ((CustomMethodSecurityExpressionRoot) ctx.getRootObject().getValue()).setReturnObject(returnObject);
    }

	@Override
	public Object filter(final Object filterTarget, final Expression filterExpression,
			final EvaluationContext ctx) {
		CustomMethodSecurityExpressionRoot rootObject = (CustomMethodSecurityExpressionRoot) ctx
				.getRootObject().getValue();

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Filtering with expression: "
					+ filterExpression.getExpressionString());
		}

		/*if (filterTarget instanceof Collection 
				|| filterTarget.getClass().isArray()) {
			return super.filter(filterTarget, filterExpression, ctx);
		}*/
		
		if (filterTarget instanceof ApplicationDescription) {
			rootObject.setFilterObject(((ApplicationDescription) filterTarget).getAuthGroups());
			if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
				return filterTarget;
			} else {
				return null;
			}
		} else if (filterTarget instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> objectsList = (List<Object>) filterTarget;
			List<Object> retainList = new ArrayList<Object>();

			for (Object objectToFilter : objectsList) {
				if (objectToFilter instanceof ApplicationDescription) {
					rootObject.setFilterObject(((ApplicationDescription) objectToFilter).getAuthGroups());
					if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
						retainList.add(objectToFilter);
					}
				}

			}
			return retainList;
			
		} else if (filterTarget instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> returnValue = (Map<String, Object>) filterTarget;

			if (CloudifyConstants.SUCCESS_STATUS.equals(returnValue.get(CloudifyConstants.STATUS_KEY))) {
				Object responseObject = returnValue.get(CloudifyConstants.RESPONSE_KEY);
				if (responseObject instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> objectsMap = (Map<String, String>) responseObject;
					Map<Object, String> retainMap = new HashMap<Object, String>();

					for (Map.Entry<String, String> entry : objectsMap.entrySet()) {
						String filterObject = entry.getValue();
						rootObject.setFilterObject(filterObject);
						if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
							retainMap.put(entry.getKey(), entry.getValue());
						}
					}
					returnValue.put(CloudifyConstants.RESPONSE_KEY, retainMap);

				} else if (responseObject instanceof List) {
					@SuppressWarnings("unchecked")
					List<Object> objectsList = (List<Object>) responseObject;
					List<Object> retainList = new ArrayList<Object>();

					for (Object object : objectsList) {
						if (object instanceof ApplicationDescription) {
							rootObject.setFilterObject(((ApplicationDescription) object).getAuthGroups());
							if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
								retainList.add(object);
							}
						}

					}
					returnValue.put(CloudifyConstants.RESPONSE_KEY, retainList);
				}
			}

			return returnValue;
		}

		throw new IllegalArgumentException(
				"Filter target must be a collection, an array or an ApplicationDescription, but was "
						+ filterTarget);
	}
}