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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.InvocationResult;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Invokes a custom command on a specific service over REST.
 * 
 *        Required arguments: service-name - The service to invoke the command on command-name - The name of the command
 *        to invoke params - Command parameters
 * 
 *        Optional arguments: beanname - Bean name instanceid - If provided, the command will be invoked only on that
 *        specific instance
 * 
 *        Command syntax: invoke [-beanname beanname] [-instanceid instanceid] service-name command-name params
 */
@Command(scope = "cloudify", name = "invoke", description = "invokes a custom command")
public class Invoke extends AdminAwareCommand {

	@Argument(index = 0, name = "service-name", required = true, description = " the service to invoke the command on")
	private String serviceName;

	@Argument(index = 1, name = "command-name", required = true, description = "the name of the command to invoke")
	private String commandName;

	@Option(name = "-beanname", description = "bean name")
	private final String beanName = "universalServiceManagerBean";

	@Option(name = "-instanceid", description = "If provided, the command will be invoked only on that specific "
			+ "instance")
	private Integer instanceId;

	@Argument(index = 2, multiValued = true, name = "params", required = false, description = "Command Custom "
			+ "parameters.")
	private final List<String> params = new ArrayList<String>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {
		// Containing all the success invocation messages.
		final StringBuilder invocationSuccessStringBuilder = new StringBuilder("Invocation results: ");
		invocationSuccessStringBuilder.append(System.getProperty("line.separator"));

		String applicationName = this.getCurrentApplicationName();
		if (applicationName == null) {
			applicationName = "default";
		}

		Map<String, String> paramsMap = new HashMap<String, String>();
		if (params != null) {
			paramsMap = getParamsMap(params);
		}

		// Containing all the failed invocation messages.
		final StringBuilder invocationFailedStringBuilder = new StringBuilder();
		if (instanceId == null) { // Invoking command on all of the service's instances.
			final Map<String, InvocationResult> result = adminFacade.invokeServiceCommand(applicationName,
					serviceName, beanName, commandName, paramsMap);

			final Collection<InvocationResult> values = result.values();
			final List<InvocationResult> valuesList = new ArrayList<InvocationResult>(values);
			Collections.sort(valuesList);

			for (final InvocationResult invocationResult : valuesList) {
				if (invocationResult.isSuccess()) {
					final String successMessage = getFormattedMessage("invocation_success",
							invocationResult.getInstanceId(), invocationResult.getInstanceName(),
							invocationResult.getResult());
					invocationSuccessStringBuilder.append(successMessage).append(System.getProperty("line.separator"));
				} else {
					final String failedMessage = getFormattedMessage("invocation_failed",
							invocationResult.getInstanceId(), invocationResult.getInstanceName(),
							invocationResult.getExceptionMessage());
					invocationFailedStringBuilder.append(failedMessage).append(System.getProperty("line.separator"));
				}
			}
		} else { // instanceID specified. invoking command on specific instance.

			final InvocationResult invocationResult = adminFacade.invokeInstanceCommand(applicationName, serviceName,
					beanName, instanceId, commandName, paramsMap);
			if (invocationResult.isSuccess()) {
				final String successMessage = getFormattedMessage("invocation_success",
						invocationResult.getInstanceId(), invocationResult.getInstanceName(),
						invocationResult.getResult());
				invocationSuccessStringBuilder.append(successMessage).append(System.getProperty("line.separator"));
			} else {
				final String failedMessage = getFormattedMessage("invocation_failed",
						invocationResult.getInstanceId(), invocationResult.getInstanceName(),
						invocationResult.getExceptionMessage());
				invocationFailedStringBuilder.append(failedMessage).append(System.getProperty("line.separator"));
			}
		}
		// print the success messages to the screen.
		logger.info(invocationSuccessStringBuilder.toString());

		if (invocationFailedStringBuilder.length() != 0) {
			throw new CLIStatusException("not_all_invocations_completed_successfully", this.serviceName,
					invocationFailedStringBuilder.toString());
		}

		return getFormattedMessage("all_invocations_completed_successfully");
	}

	// TODO: look at karaf's MultiValue option
	private Map<String, String> getParamsMap(final List<String> parameters) {
		int index = 0;
		final Map<String, String> returnMap = new HashMap<String, String>();
		for (final String param : parameters) {
			returnMap.put(CloudifyConstants.INVOCATION_PARAMETERS_KEY + index, param);
			++index;
		}

		return returnMap;
	}
}
