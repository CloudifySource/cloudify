package org.cloudifysource.usm;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.PreStartListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.liveness.LivenessDetector;

public class TCPPortEventListener implements PreStartListener, LivenessDetector {

	private int port;

	public TCPPortEventListener(final int port) {
		this.port = port;
	}

	@Override
	public void init(UniversalServiceManagerBean usm) {

	}

	@Override
	public int getOrder() {
		return 5;
	}

	@Override
	public boolean isProcessAlive() throws USMException, TimeoutException {
		return ServiceUtils.isPortOccupied(port);
	}

	@Override
	public EventResult onPreStart(StartReason reason) {
		return (ServiceUtils.isPortFree(port) ? EventResult.SUCCESS
				: new EventResult(
						new IOException(
								"Port "
										+ port
										+ " which is required for this service is already in use!")));
	}

}
