package org.cloudifysource.usm.shutdown;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.gigaspaces.internal.sigar.SigarHolder;

public class DefaultProcessKiller implements ProcessKiller {

	private static final int KILL_RETRIES = 2;
	private static final int POST_KILL_SLEEP_INTERVAL = 100;
	private static final int PROCESS_STATUS_CHECK_INTERVAL = 200;
	private static final int PROCESS_STATUS_CHECK_ATTEMPTS = 5;

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(DefaultProcessKiller.class.getName());

	@Override
	public void killProcess(final long pid) throws USMException {
		if (pid == 0) {
			return; // this is possible in some end case situations, in the
					// IntegratedProcessingUnitContainer
		}
		final Sigar sigar = SigarHolder.getSigar();

		for (int retries = 0; retries <= KILL_RETRIES; ++retries) {
			try {
				logger.info("Killing process: " + pid);
				sigar.kill(pid, "SIGTERM"); // (9 is the only signal used for
											// Kill on windows)
			} catch (final SigarException e) {
				logger.warning("Failed to shut down process: " + pid + ". Process may already be dead. Error was: "
						+ e.getMessage() + ".");
			}

			try {
				// sleep for a short period so process table get cleaned up
				Thread.sleep(POST_KILL_SLEEP_INTERVAL);
			} catch (final InterruptedException e1) {
				// ignore
			}

			// wait until process dies
			for (int i = 0; i < PROCESS_STATUS_CHECK_ATTEMPTS; ++i) {
				if (!USMUtils.isProcessAlive(pid)) {
					logger.info("Process " + pid + " is dead");
					return;
				}
				try {
					Thread.sleep(PROCESS_STATUS_CHECK_INTERVAL);
				} catch (final InterruptedException e) {
					// ignore
				}
			}
			logger.warning("Attempt number " + (retries + 1) + " to kill process " + pid + " failed.");
		}

		logger.severe("Process " + pid + " did not die as expected!");

		throw new USMException("Attempt to kill process " + pid + " failed!");

	}


}
