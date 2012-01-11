package org.cloudifysource.usm.details;

public class DetailsException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DetailsException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public DetailsException(String msg) {
		super(msg);
	}
	
	public DetailsException(Throwable cause) {
		super(cause);
	}
}
