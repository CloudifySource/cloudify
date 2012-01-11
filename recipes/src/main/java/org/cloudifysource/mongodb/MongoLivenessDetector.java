package org.cloudifysource.mongodb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.PreStartListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.liveness.LivenessDetector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.TimeoutException;


public class MongoLivenessDetector extends AbstractMongoPlugin implements LivenessDetector, PreStartListener {

	private static final String TIMEOUT_KEY = "timeoutInSeconds";
	private static final int DEFAULT_TIMEOUT_SECONDS  = 60;

	private final Log log = LogFactory.getLog(getClass().getName());

	private int timeoutInSeconds = DEFAULT_TIMEOUT_SECONDS ;


    public void init() {
        if(config.get(TIMEOUT_KEY) != null) {
            timeoutInSeconds = (Integer) config.get(TIMEOUT_KEY);
        }
        super.init();
	}
	
	/**
	 * isProcessAlive will repeatedly try to connect to the ports defined in the groovy configuration file
	 * every second for the specified timeout period to see whether the ports are open.
	 * Having all the tested ports opened means that the process has completed loading successfully.
	 * @throws USMException 
	 * 
	 */
	public boolean isProcessAlive() throws TimeoutException {
        if (!initialized) init();
        ServiceUtils.isPortsOccupied(Collections.singletonList(port));
        return true;
	}

	public void init(UniversalServiceManagerBean usm) {}

	public int getOrder() {
		return 5;
	}

	public EventResult onPreStart(StartReason reason) {
        if (!initialized) init();
		Socket sock = new Socket();
        try {
            sock.connect(new InetSocketAddress(host, port));
            sock.close();
            throw new IllegalStateException(
                    "The Port Liveness Detector found that port " + port
                    + " is IN USE before the process was launched!");
        } catch (IOException e) {
            log.debug("The Port Liveness Detector found that the port " + port +
            " is FREE and can be used by the process");
        }finally {
            try {
                sock.close();
            } catch (IOException e) {
                // ignore
            }
        }
		return EventResult.SUCCESS;
	}
}
