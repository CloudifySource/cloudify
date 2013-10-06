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

import java.util.Arrays;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.usm.USMLifecycleBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * a built-in command used to enables the GSA failure detection of the PU instance.
 * @author adaml
 * @since 2.7.0
 *
 */
@Component
public class StopMaintenanceMode implements USMBuiltInCommand, InitializingBean {

	private static final  String NAME = "stop-maintenance-mode";
	private static final String SUCCESS_MESSAGE = "agent failure detection enabled successfully.";
	
	@Autowired(required = true)
	private USMLifecycleBean usmLifecycleBean;
	
	private ServiceContext context;

	@Override
	public Object invoke(final Object... args) {
		verifyParams(args);
		this.getContext().stopMaintenanceMode();
    	return SUCCESS_MESSAGE;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.setContext(usmLifecycleBean.getConfiguration().getServiceContext());
	}

	void verifyParams(final Object... args) {
		if (args.length != 0) {
			throw new IllegalArgumentException("command " + NAME + " does not accept parameters. received " 
						+ Arrays.toString(args));
		}
	}

	@Override
	public String getName() {
		return NAME;
	}

	public ServiceContext getContext() {
		return context;
	}

	public void setContext(ServiceContext context) {
		this.context = context;
	}
}
