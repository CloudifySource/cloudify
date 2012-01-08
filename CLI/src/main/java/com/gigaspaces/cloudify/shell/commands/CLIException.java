package com.gigaspaces.cloudify.shell.commands;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import com.gigaspaces.cloudify.restclient.ErrorStatusException;
import com.gigaspaces.cloudify.shell.ShellUtils;

public class CLIException extends Exception {

	private static final long serialVersionUID = 1295396747968774683L;
	private String reasonCode;
	private Object[] args;

	public CLIException() {
	}

	public CLIException(String message) {
		super(message);
		//check if the message is in the messages bundle (i.e. it's a reason code)
		try{ 
			if (StringUtils.isNotBlank(message)) {
				ResourceBundle messages = ShellUtils.getMessageBundle();
				String messageFromBundle = messages.getString(message);
				if (StringUtils.isNotBlank(messageFromBundle))
					this.reasonCode = message;
			}
		} catch (MissingResourceException mre) {
			// the message is not in a resource bundle - that's OK
		}

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
