package org.cloudifysource.usm;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.PreStartListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.liveness.LivenessDetector;

/*****************
 * A USM component that checks if the network port required by a service, as defined in its network block, is free in
 * the preStart phase, and is in use in the start detection phase. This class is a good way to avoid some boierplate
 * code in service files.
 * 
 * @author barakme
 * 
 */
public class TCPPortEventListener implements PreStartListener, LivenessDetector {

	private static final int DEFAULT_ORDER = 5;
	private final int port;

	/**************
	 * Constructor.
	 * @param port the port number specified in the service network block.
	 */
	public TCPPortEventListener(final int port) {
		this.port = port;
	}

	@Override
	public void init(final UniversalServiceManagerBean usm) {

	}

	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public boolean isProcessAlive()
			throws USMException, TimeoutException {
		return ServiceUtils.isPortOccupied(port);
	}

	@Override
	public EventResult onPreStart(final StartReason reason) {
		return ServiceUtils.isPortFree(port) ? EventResult.SUCCESS
				: new EventResult(
						new IOException(
								"Port "
										+ port
										+ " which is required for this service is already in use!"));
	}

}
