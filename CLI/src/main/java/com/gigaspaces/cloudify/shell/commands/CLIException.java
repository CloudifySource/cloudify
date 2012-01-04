package com.gigaspaces.cloudify.shell.commands;

import com.gigaspaces.cloudify.restclient.ErrorStatusException;

public class CLIException extends Exception {

	private static final long serialVersionUID = 1295396747968774683L;
	private String reasonCode;
	private Object[] args;

	public CLIException() {
	}

	public CLIException(String message) {
		super(message);

	}

	public CLIException(Throwable cause) {
		super(cause);
		if (cause instanceof ErrorStatusException) {
			this.args = ((ErrorStatusException)cause).getArgs();
			this.reasonCode = ((ErrorStatusException)cause).getReasonCode();
		}
	}

	public CLIException(String message, Throwable cause) {
		super(message, cause);
	}

	public CLIException(String reasonCode, Throwable cause,
			Object... args) {
		super("reasonCode: " + reasonCode, cause);
		this.args = args;
		this.reasonCode = reasonCode;
	}

	public CLIException(String reasonCode, Object... args) {
		super("reasonCode: " + reasonCode);
		this.reasonCode = reasonCode;
		this.args = args;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public Object[] getArgs() {
		return args;
	}

}
