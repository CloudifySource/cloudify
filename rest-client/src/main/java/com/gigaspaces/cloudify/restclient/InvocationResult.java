package com.gigaspaces.cloudify.restclient;

import java.util.Map;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;

public class InvocationResult implements Comparable<InvocationResult> {


	private String exceptionMessage;
	private String result;
	private String commandName;
	private int instanceId;
	private String instanceName;
	private boolean success;

	public static InvocationResult createInvocationResult(Map<String, String> map) {
		
		InvocationResult res = new InvocationResult();
		res.commandName = map.get(CloudifyConstants.INVOCATION_RESPONSE_COMMAND_NAME);
		res.exceptionMessage = map.get(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION);
		res.instanceId = Integer.parseInt(map.get(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_ID));
		res.result = map.get(CloudifyConstants.INVOCATION_RESPONSE_RESULT);
		res.success = Boolean.parseBoolean(map.get(CloudifyConstants.INVOCATION_RESPONSE_STATUS));
		res.instanceName= map.get(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_NAME);
		
		return res;
	}
	public String getInstanceName() {
		return instanceName;
	}
	public InvocationResult() {
		
		
	}
	
	
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public String getResult() {
		return result;
	}

	public String getCommandName() {
		return commandName;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public boolean isSuccess() {
		return success;
	}
	@Override
	public int compareTo(InvocationResult o) {
		return this.instanceId - o.instanceId;
	}	
	
}
