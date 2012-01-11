package org.cloudifysource.usm.liveness;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.USMEvent;



public interface LivenessDetector extends USMEvent {

	public boolean isProcessAlive() throws USMException, TimeoutException; 
}
