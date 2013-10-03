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
package org.cloudifysource.usm.commands;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.usm.USMLifecycleBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * a built-in command used to disable the GSA failure detection of the calling PU instance.
 * @author adaml
 *
 */
@Component
public class StartMaintenanceMode implements BuiltInCommand, InitializingBean {
	
	private static final String name = "start-maintenance-mode";
	private static final String successMessage = "agent failure detection disabled" 
												+ " successfully for a period of {0} minutes";
	
	@Autowired(required = true)
	private USMLifecycleBean usmLifecycleBean;
	
	private ServiceContext context;

	@Override
	public Object invoke(final Object... params) {
		validateParams(params);
		getContext().startMaintenanceMode(Long.parseLong(params[0].toString()), TimeUnit.MINUTES);
		return MessageFormat.format(successMessage, params[0].toString());
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.setContext(usmLifecycleBean.getConfiguration().getServiceContext());
	}
	
	private void validateParams(final Object... params) {
		if (params.length != 1) {
			throw new IllegalArgumentException("command " + name + " requires one param of type 'long', got " 
											+ Arrays.toString(params));
		}
		try {
			Long.parseLong(params[0].toString());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("parameter type mismatch. can't convert " 
														+ params[0].getClass() + " to 'long'", e);
		}
	}

	public ServiceContext getContext() {
		return context;
	}

	public void setContext(final ServiceContext context) {
		this.context = context;
	}
}
