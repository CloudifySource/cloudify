/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.util.concurrent.RateLimiter;

/**
 * This rate limiter is designed to allow a specific number of request attempts in a defined period of time.
 * in-case the request limit exceeds, block method will block the current thread until the cool-down 
 * period has ended. The implementation uses the {@link com.google.common.util.concurrent.RateLimiter}
 * to determine the actual cool-down period of the throttling according to the time frame specified.
 * 
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public class RequestRateLimiter {
	
	private final int numRequests;
	private final RateLimiter throttler;
	private final Logger logger = Logger.getLogger(RequestRateLimiter.class.getName());

	private int requestCounter;
	
	/**
	 * 
	 * @param numRequests - the number of allowed requests in the given time frame.
	 * @param timeFrame - the timeframe limit for number of requests
	 * @param unit - time unit.
	 */
	public RequestRateLimiter(final int numRequests, final long timeFrame, final TimeUnit unit) {
		this.numRequests = numRequests;
		this.throttler = RateLimiter.create(1.0 / unit.toSeconds(timeFrame));
	}

	/**
	 * initializes the request rate limiter upon first block request in rotation. 
	 */
	private void init() {
		boolean acquired = this.throttler.tryAcquire();
		if (acquired) {
			this.throttler.acquire();
		}
	}
	
	/**
	 * 
	 * @return true if blocking was preformed.
	 */
	public boolean block() {
		incrementRetryCounter();
		if (isInitRequired()) {
			init();
		}
		if (isThrottlingRequired()) {
			logger.fine("Request limit has been reached. Rate limit is being enforced.");
			throttler.acquire();
			logger.fine("Rate limit enforcement has ended.");
			return true;
		}
		return false;
	}
	
	// is this the first block attempt in the rotation?
	private boolean isInitRequired() {
		if ((this.requestCounter % this.getNumRequests()) == 1) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return true if blocking is required.
	 */
	public boolean tryBlock() {
		return isThrottlingRequired();
	}

	private boolean isThrottlingRequired() {
		if ((this.requestCounter % this.getNumRequests()) == 0) {
			return true;
		}
		return false;
	}

	private synchronized void incrementRetryCounter() {
		this.requestCounter = this.requestCounter + 1;
	}
	
	/**
	 * Returns the defined duration for consequtive requests.
	 * 
	 * @return the defined duration for consequtive requests.
	 */
	public long getDuration() {
		return (long) (1.0 / this.throttler.getRate());
	}

	public int getNumRequests() {
		return numRequests;
	}
	
	/**
	 * returns the remaining retries until block.
	 * 
	 * @return 
	 * 		remaining retries until block.
	 */
	public int getRemainingRetries() {
		return numRequests - (this.requestCounter % numRequests);
	}
}
