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
package org.cloudifysource.shell;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.shell.commands.CLIException;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        The ConditionLatch waits for a specific process (defined as a {@link Predicate}) to complete. It
 *        samples its status according to a specified polling interval and if the process is not completed
 *        before the specified timeout is reached, a {@link TimeoutException} is thrown, with the configured
 *        error message.
 */
public class ConditionLatch {

	private static final long DEFAULT_INTERVAL_SECONDS = 10;
	private static final String DEFAULT_TIMEOUT_ERROR_MESSAGE = "Operation timed out";

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private String timeoutErrorMessage = DEFAULT_TIMEOUT_ERROR_MESSAGE;
	private long pollingIntervalMilliseconds = TimeUnit.SECONDS.toMillis(DEFAULT_INTERVAL_SECONDS);
	private boolean verbose = false;
	private long timeoutMilliseconds;

	/**
	 * 
	 * Predicate interface defines a single method to be implemented - isDone(). This method is required for
	 * the condition latch to monitor the predicate's status.
	 * 
	 */
	public interface Predicate {
		/**
		 * Gets the predicate's status.
		 * 
		 * @return status (true - done, false - not done)
		 * @throws CLIException
		 *             Reporting a failure to get the status
		 * @throws InterruptedException
		 *             Reporting the thread was interrupted while waiting
		 */
		boolean isDone() throws CLIException, InterruptedException;
	}

	/**
	 * Sets the error message of the timeout exception, thrown when a predicate is not done before the timeout
	 * is reached.
	 * 
	 * @param timeoutErrorMessage
	 *            The error message to be attached to the timeout exception
	 * @return This instance of {@link ConditionLatch}, configured with the specified error message
	 */
	public ConditionLatch timeoutErrorMessage(final String timeoutErrorMessage) {
		this.timeoutErrorMessage = timeoutErrorMessage;
		return this;
	}

	/**
	 * Sets the interval for polling the predicate's status.
	 * 
	 * @param duration
	 *            The number of {@link TimeUnit}s to use
	 * @param timeunit
	 *            The time unit to use (seconds, minutes etc.)
	 * @return This instance of {@link ConditionLatch}, configured with the specified polling interval
	 */
	public ConditionLatch pollingInterval(final long duration, final TimeUnit timeunit) {
		this.pollingIntervalMilliseconds = timeunit.toMillis(duration);
		return this;
	}

	/**
	 * Sets the verbose mode, setting on/off the logging while the predicate is monitored.
	 * 
	 * @param verbose
	 *            Verbose mode for this condition latch (true - on, false - off)
	 * @return This instance of {@link ConditionLatch}, configured with the specified verbose mode
	 */
	public ConditionLatch verbose(final boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	/**
	 * Calculates and sets the timeout for this condition latch.
	 * 
	 * @param timeout
	 *            The number of {@link TimeUnit}s to calculate
	 * @param timeunit
	 *            The time unit to use (seconds, minutes etc.)
	 * @return This instance of {@link ConditionLatch}, configured with the specified timeout
	 */
	public ConditionLatch timeout(final long timeout, final TimeUnit timeunit) {
		this.timeoutMilliseconds = timeunit.toMillis(timeout);
		return this;
	}

	/**
	 * Waits for the given predicate to complete. The predicate is monitored according to the specified
	 * polling interval. If the timeout is reached before the predicate is done, a timeout exception is
	 * thrown.
	 * 
	 * @param predicate
	 *            The predicate to monitor
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 * @throws CLIException
	 *             Reporting a failure to monitor the predicate's status
	 */
	public void waitFor(final Predicate predicate) throws InterruptedException, TimeoutException, CLIException {

		final long end = System.currentTimeMillis() + timeoutMilliseconds;

		boolean isDone = predicate.isDone();
		while (!isDone && System.currentTimeMillis() < end) {
			if (verbose) {
				logger.log(Level.FINE,
						"\nnext check in " + TimeUnit.MILLISECONDS.toSeconds(pollingIntervalMilliseconds) + " seconds");
			}
			Thread.sleep(pollingIntervalMilliseconds);
			isDone = predicate.isDone();
		}

		if (!isDone && System.currentTimeMillis() >= end) {
			throw new TimeoutException(timeoutErrorMessage);
		}
	}

}
