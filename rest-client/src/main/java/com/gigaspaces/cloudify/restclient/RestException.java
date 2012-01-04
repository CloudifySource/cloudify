package com.gigaspaces.cloudify.restclient;

public class RestException extends Exception {
	
	private static final long serialVersionUID = -7304916239245226345L;

	public RestException() {
	}

	public RestException(String message) {
		super(message);

	}

	public RestException(Throwable cause) {
		super(cause);
	}

	public RestException(String message, Throwable cause) {
		super(message, cause);
	}
}
