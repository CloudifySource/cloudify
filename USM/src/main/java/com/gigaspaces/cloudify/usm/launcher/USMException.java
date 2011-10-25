package com.gigaspaces.cloudify.usm.launcher;

public class USMException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public USMException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public USMException(String msg) {
		super(msg);
	}
	
	public USMException(Throwable cause) {
		super(cause);
	}

}
