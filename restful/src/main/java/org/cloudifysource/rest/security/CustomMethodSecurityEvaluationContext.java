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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;

/**
 * 
 * @author noak
 * @since 2.3.0
 *
 */
public class CustomMethodSecurityEvaluationContext extends StandardEvaluationContext {
	 private Logger logger = java.util.logging.Logger.getLogger(CustomMethodSecurityEvaluationContext.class.getName());

    private ParameterNameDiscoverer parameterNameDiscoverer;
    private boolean argumentsAdded;
    private MethodInvocation mi;

    /**
     * Intended for testing. Don't use in practice as it creates a new parameter resolver
     * for each instance. Use the constructor which takes the resolver, as an argument thus
     * allowing for caching.
     */
    public CustomMethodSecurityEvaluationContext(final Authentication user, final MethodInvocation mi) {
        this(user, mi, new LocalVariableTableParameterNameDiscoverer());
    }

    public CustomMethodSecurityEvaluationContext(final Authentication user, final MethodInvocation mi,
                    final ParameterNameDiscoverer parameterNameDiscoverer) {
        this.mi = mi;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    @Override
    public Object lookupVariable(final String name) {
        Object variable = super.lookupVariable(name);
        if (variable != null) {
            return variable;
        }

        if (!argumentsAdded) {
            addArgumentsAsVariables();
            argumentsAdded = true;
        }

        return super.lookupVariable(name);
    }

    public void setParameterNameDiscoverer(final ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    private void addArgumentsAsVariables() {
        Object[] args = mi.getArguments();

        if (args.length == 0) {
            return;
        }

        Object targetObject = mi.getThis();
        Method method = AopUtils.getMostSpecificMethod(mi.getMethod(), targetObject.getClass());
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames == null) {
            logger.warning("Unable to resolve method parameter names for method: " + method
                    + ". Debug symbol information is required if you are using parameter names in expressions.");
            return;
        }

        for (int i = 0; i < args.length; i++) {
            super.setVariable(paramNames[i], args[i]);
        }
    }
}
