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
package org.cloudifysource.shell.commands;

import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;

@Command(scope = "cloudify", name = "set-instances", description = "Sets the number of services of an elastic service")
public class SetInstances extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 1;

	@Argument(index = 0, name = "service-name", required = true, description = "the service to scale")
	private String serviceName;

	@Argument(index = 1, name = "count", required = true, description = "the target number of instances")
	private int count;

	@Option(required = false, name = "-timeout", description = "number of minutes to wait for instances. Default is set to 1 minute")
	protected int timeout = DEFAULT_TIMEOUT_MINUTES;
	
	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. " 
				+ "Try to increase the timeout using the -timeout flag";

	// THIS DOES NOT WORK
	// The Karaf @CompleterValues annotation only works on statis lists - the method is only invoked once, on loading of
	// the commands!
	// @CompleterValues(index = 1)
	// public Collection<String> getCompleterValues() throws Exception {
	// try {
	// System.out.println("getting possible completion values");
	// String applicationName = this.getCurrentApplicationName();
	// if (applicationName == null) {
	// applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
	// }
	//
	// List<String> services = adminFacade.getServicesList(applicationName);
	//
	// System.out.println("Returning: " + services);
	// return services;
	// } catch (Exception e) {
	// System.out.println(e);
	// throw e;
	// }
	//
	// }

	@Override
	protected Object doExecute() throws Exception {
		String applicationName = this.getCurrentApplicationName();
		if (applicationName == null) {
			applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
		}

		final int initialNumberOfInstances = adminFacade.getInstanceList(applicationName, serviceName).size();
		if (initialNumberOfInstances == count) {
			return getFormattedMessage("num_instanes_already_met", count);
		}

		Map<String, String> response = adminFacade.setInstances(applicationName, serviceName, count, timeout);

		if (response.containsKey(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID)) {
			String pollingID = response.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
			this.adminFacade.waitForLifecycleEvents(pollingID, timeout, TIMEOUT_ERROR_MESSAGE);
		} else {
			logger.info("Failed to retrieve lifecycle logs from rest. " 
			+ "Check logs for more details.");
		}
		
		return getFormattedMessage("set_instances_completed_successfully", serviceName, count);
	}

}
