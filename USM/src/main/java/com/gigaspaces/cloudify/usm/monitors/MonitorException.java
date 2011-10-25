package com.gigaspaces.cloudify.usm.monitors;

public class MonitorException extends Exception {
	public MonitorException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public MonitorException(String msg) {
		super(msg);
	}
	
	public MonitorException(Throwable cause) {
		super(cause);
	}
}
