package org.cloudifysource.usm.stopDetection;

import java.util.logging.Logger;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;


public class ProcessStopDetector implements StopDetector {

	private UniversalServiceManagerBean usm;

	private final static Logger logger = Logger.getLogger(ProcessStopDetector.class.getName());
	@Override
	public boolean isServiceStopped() throws USMException {
		final long pid = usm.getActualProcessID();
		final boolean processStopped = !USMUtils.isProcessAlive(pid);
		if(processStopped) {
			logger.warning("Process Stop Detector execution has found that process: " + pid + " is not alive!");
		}
		return processStopped;
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
