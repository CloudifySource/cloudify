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
 ******************************************************************************/
package org.cloudifysource.restclient;

import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;



/**
 * This is a POJO holding command execution and result details, as returned from
 * a rest command invoker.
 * <p>
 * (Commands are executed over rest through the post methods in
 * {@link org.cloudifysource.restclient.GSRestClient}, and returned as a
 * Map object, parsed here.)
 */
public class InvocationResult implements Comparable<InvocationResult> {

	/**
	 * Represents the result status - a reported success, a reported failure, or an unexpected return value.
	 * @author noak
	 *
	 */
	public enum InvocationStatus {
		/**
		 * The invocation reported a successful execution of the command.
		 */
		SUCCESS,
		
		/**
		 * The invocation reported a failed execution of the command.
		 */
		FAILURE,
		
		/**
		 * The invocation resulted in an unexpected return value.
		 */
		UNEXPECTED;
	}
	
	/**
	 * private members.
	 */
	private String exceptionMessage;
	private String result;
	private String commandName;
	private int instanceId;
	private String instanceName;
	private InvocationStatus invocationStatus;

	/**
	 * Default empty Ctor.
	 */
	public InvocationResult() {
	}
	

	/**
	 * @param map
	 *            The Map object returned from the rest call, holding command
	 *            execution and result details
	 * @return InvocationResult consisting of the data in the given map.
	 */
	public static InvocationResult createInvocationResult(final Map<String, String> map) {

		InvocationResult res = new InvocationResult();
		res.commandName = map
				.get(CloudifyConstants.INVOCATION_RESPONSE_COMMAND_NAME);
		res.exceptionMessage = map
				.get(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION);
		res.instanceId = Integer.parseInt(map
				.get(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_ID));
		res.result = map.get(CloudifyConstants.INVOCATION_RESPONSE_RESULT);
		if (Boolean.parseBoolean(map
				.get(CloudifyConstants.INVOCATION_RESPONSE_STATUS))) {
			res.invocationStatus = InvocationStatus.SUCCESS;
		} else {
			res.invocationStatus = InvocationStatus.FAILURE;
		}
		res.instanceName = map
				.get(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_NAME);

		return res;
	}

	/**
	 * @param instanceName
	 *            The name of the instance the command was invoked on
	 * @param result
	 *            The result of the invoked command
	 * @return InvocationResult containing the returned result, as string
	 */
	public static InvocationResult createInvocationResult(final String instanceName, final Object result) {

		InvocationResult res = new InvocationResult();
		res.invocationStatus = InvocationStatus.UNEXPECTED;
		res.instanceName = instanceName;
		res.result = result.toString();

		return res;
	}
	
	/**
	 * Gets the name of the instance this invocation occurred on.
	 * 
	 * @return instance name as a String
	 */
	public final String getInstanceName() {
		return instanceName;
	}

	/**
	 * Gets the exception message.
	 * 
	 * @return exception message as a String
	 */
	public final String getExceptionMessage() {
		return exceptionMessage;
	}

	/**
	 * Gets the invocation's result.
	 * 
	 * @return the invocation's result as a String
	 */
	public final String getResult() {
		return result;
	}

	/**
	 * Gets the invoked command's name.
	 * 
	 * @return the command name as a String
	 */
	public final String getCommandName() {
		return commandName;
	}

	/**
	 * Gets the id of the instance this invocation occurred on.
	 * 
	 * @return instance id
	 */
	public final int getInstanceId() {
		return instanceId;
	}

	/**
	 * Gets the invocation's result as an {@link InvocationStatus} object.
	 * 
	 * @return the invocation's result as an InvocationStatus object
	 */
	public final InvocationStatus getInvocationStatus() {
		return invocationStatus;
	}
	
	/**
	 * Returns true if the status is succcess. false otherwise (status is failure/unexpetced).
	 * @return true / false
	 */
	public final boolean isSuccess() {
		return (invocationStatus == InvocationStatus.SUCCESS);
	}

	@Override
	public final int compareTo(final InvocationResult o) {
		return this.instanceId - o.instanceId;
	}

}
