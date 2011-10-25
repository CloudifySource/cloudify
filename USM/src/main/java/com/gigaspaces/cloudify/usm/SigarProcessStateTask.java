package com.gigaspaces.cloudify.usm;

import java.util.logging.Level;

import com.gigaspaces.cloudify.usm.launcher.USMException;

public class SigarProcessStateTask implements Runnable {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(SigarProcessStateTask.class.getName());

	private final long pid;
	private final ProcessDeathNotifier notifier;
	private final long intervalMs;

	public SigarProcessStateTask(final long pid,
			final ProcessDeathNotifier notifier, final long intervalMs) {
		super();
		this.pid = pid;
		this.notifier = notifier;
		this.intervalMs = intervalMs;
	}

	public void run() {

		try {
			while (true) {
				try {
					if (!USMUtils.isProcessAlive(pid)) {
						notifier.processDeathDetected();
						return;
					}
				} catch (final USMException e) {
					logger.log(Level.WARNING, "failed to check if process: " + pid + " is alive", e);

				}
				Thread.sleep(intervalMs);

			}
		} catch (final InterruptedException e) {
			// ignore
		}

	}

}
