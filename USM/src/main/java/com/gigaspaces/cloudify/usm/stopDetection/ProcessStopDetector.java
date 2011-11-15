package com.gigaspaces.cloudify.usm.stopDetection;

import com.gigaspaces.cloudify.usm.USMException;
import com.gigaspaces.cloudify.usm.USMUtils;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;

public class ProcessStopDetector implements StopDetector {

	private UniversalServiceManagerBean usm;

	@Override
	public boolean isServiceStopped() throws USMException {
		final long pid = usm.getActualProcessID();
		return !USMUtils.isProcessAlive(pid);
	}

	@Override
	public void init(UniversalServiceManagerBean usm) {
		this.usm = usm;
	}

	@Override
	public int getOrder() {
		return 5;
	}

}
