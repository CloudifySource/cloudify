package com.gigaspaces.cloudify.usm.events;

public class EventResult {

	
	
	private boolean success;
	private Exception exception;
	private Object result;
	
	public static final EventResult SUCCESS = new EventResult(true, null); 
	
	public EventResult(Exception exception) {
		this.success = false;
		this.exception = exception;
	}
	
	public EventResult(Object result) {
		this.success = true;
		this.result = result;
		this.exception = null;
		
	}
	public EventResult(boolean success, Exception exception) {
		super();
		this.success = success;
		this.exception = exception;
	}
	
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
