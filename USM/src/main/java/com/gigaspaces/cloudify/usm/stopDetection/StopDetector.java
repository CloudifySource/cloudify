package com.gigaspaces.cloudify.usm.stopDetection;

import com.gigaspaces.cloudify.usm.USMException;
import com.gigaspaces.cloudify.usm.events.USMEvent;


public interface StopDetector extends USMEvent{
	public boolean isServiceStopped() throws USMException;

}
