package org.cloudifysource.shell.commands;

public class CLIException extends Exception {

	private static final long serialVersionUID = 1295396747968774683L;
	
	public CLIException() {
	}

	public CLIException(String message) {
		super(message);

	}

	public CLIException(Throwable cause) {
		super(cause);
	}

	public CLIException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
