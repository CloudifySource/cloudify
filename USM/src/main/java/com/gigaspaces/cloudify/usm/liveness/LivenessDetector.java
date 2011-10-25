package com.gigaspaces.cloudify.usm.liveness;

import java.util.concurrent.TimeoutException;


import com.gigaspaces.cloudify.usm.events.USMEvent;
import com.gigaspaces.cloudify.usm.launcher.USMException;

public interface LivenessDetector extends USMEvent {

	public boolean isProcessAlive() throws USMException, TimeoutException; 
}
