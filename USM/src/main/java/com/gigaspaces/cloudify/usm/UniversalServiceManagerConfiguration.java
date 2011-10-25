package com.gigaspaces.cloudify.usm;

public interface UniversalServiceManagerConfiguration {
	
	// TODO - Remove dead commands from this interface

	Object getStartCommand();

	int getNumberOfLaunchRetries();

	String getPidFile();
	
	String getServiceName();
	
	long getStartDetectionTimeoutMSecs();
	long getStartDetectionIntervalMSecs();
}
