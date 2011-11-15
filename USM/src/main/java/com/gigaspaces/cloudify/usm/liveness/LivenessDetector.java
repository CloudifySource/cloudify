package com.gigaspaces.cloudify.usm.liveness;

import java.util.concurrent.TimeoutException;


import com.gigaspaces.cloudify.usm.USMException;
import com.gigaspaces.cloudify.usm.events.USMEvent;

public interface LivenessDetector extends USMEvent {

	public boolean isProcessAlive() throws USMException, TimeoutException; 
}
