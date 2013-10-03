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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * a built-in command used to enables the GSA failure detection of the PU instance.
 * @author adaml
 *
 */
@Component
public class StopMaintenanceMode implements BuiltInCommand {

	private static final  String name = "stop-maintenance-mode";
	private static final String successMessage = "agent failure detection enabled successfully.";
	
	@Autowired(required = true)
	private USMLifecycleBean usmLifecycleBean;

	@Override
	public Object invoke(final Object... args) {
		verifyParams(args);
		final ServiceContext serviceContext = usmLifecycleBean.getConfiguration().getServiceContext();
		serviceContext.stopMaintenanceMode();
    	return successMessage;
		
	}

	void verifyParams(final Object... args) {
		if (args.length != 0) {
			throw new IllegalStateException("command " + name + " does not accept parameters. received " 
						+ Arrays.toString(args));
		}
	}

	@Override
	public String getName() {
		return name;
	}
}
