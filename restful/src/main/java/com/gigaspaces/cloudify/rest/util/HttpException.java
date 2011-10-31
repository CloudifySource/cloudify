package com.gigaspaces.cloudify.rest.util;

import org.springframework.http.HttpStatus;

public abstract class HttpException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	final HttpStatus status;
	
	public HttpException(HttpStatus status) {
		super("Http error code " + status.value());
		this.status = status;
	}
}
