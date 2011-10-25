package org.openspaces.shell.commands;

public class CLIException extends Exception {

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
