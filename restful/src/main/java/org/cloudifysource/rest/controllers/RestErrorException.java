package org.cloudifysource.rest.controllers;

import java.util.Map;

import org.cloudifysource.rest.util.RestUtils;

public class RestErrorException extends Exception{

	private Map<String, Object> errorDescription;

	public RestErrorException(String errorDesc) {
		this.errorDescription = RestUtils.errorStatus(errorDesc);
	}
	
	public RestErrorException(String errorDesc, String... args) {
		this.errorDescription = RestUtils.errorStatus(errorDesc, args);
	}
	
	public Map<String, Object> getErrorDescription() {
		return this.errorDescription;
	}
	
}
