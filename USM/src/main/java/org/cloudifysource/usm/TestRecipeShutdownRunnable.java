package org.cloudifysource.usm;

import java.util.logging.Level;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants.USMState;

class TestRecipeShutdownRunnable implements Runnable {

	private final UniversalServiceManagerBean usm;
	private final ApplicationContext applicationContext;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(TestRecipeShutdownRunnable.class.getName());

	public TestRecipeShutdownRunnable(ApplicationContext applicationContext,
			UniversalServiceManagerBean usm) {
		this.usm = usm;
		this.applicationContext = applicationContext;
	}

	@Override
	public void run() {
		logger.info("Test Recipe automatic shutdown has started");
		USMState state = usm.getState();
		if (!state.equals(USMState.RUNNING)) {
			logger.warning("Test Recipe automatic shutdown has started, but the USM is in state: "
					+ state.toString()
					+ ". Is the test timeout too short? Process will be shut down forcefully, and the service stop lifecycle will not be executed.");
			System.exit(1);
		}
		boolean shutdownSuccess = true;
		try {

			if (applicationContext instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) applicationContext).close();
			} else {
				logger.warning("Test Recipe is shutting down but the application context is of type: "
						+ this.applicationContext.getClass().getName()
						+ " and does not extend AbstractApplicationContext. The application context will not be closed, only the USM will be shut down");
				usm.shutdown();
			}
		} catch (Exception e) {
			shutdownSuccess = false;

			logger.log(
					Level.SEVERE,
					"Test Recipe automatic shutdown was invoked, but the USM shutdown failed",
					e);
		}

		if (shutdownSuccess) {
			System.exit(0);
		} else {
			System.exit(1);
		}

	}

}