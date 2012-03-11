/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm;

import java.util.logging.Level;


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

	@Override
	public void run() {

		try {
			while (true) {
				try {
					if (!USMUtils.isProcessAlive(pid)) {
						logger.info("Process death detected by Sigar monitor for process: " + pid);
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
