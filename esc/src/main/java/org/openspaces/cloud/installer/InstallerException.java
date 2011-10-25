package org.openspaces.cloud.installer;


/***********
 * An exception for failures during the agentless installation process.
 * @author barakme
 *
 */
public class InstallerException extends Exception {

	/**
	 * .
	 */
	private static final long serialVersionUID = 1L;


	/************
	 * .
	 * @param message the message.
	 */
	public InstallerException(final String message) {
		super(message);
	}

	/****
	 * .
	 * @param message .
	 * @param cause .
	 */
	public InstallerException(final String message, final Throwable cause) {
		super(message, cause);
	}
	

}
