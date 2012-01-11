package org.cloudifysource.usm.stopDetection;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.USMEvent;


public interface StopDetector extends USMEvent{
	public boolean isServiceStopped() throws USMException;

}
