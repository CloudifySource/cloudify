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

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.util.concurrent.RateLimiter;

/**
 * test start machine throttler.
 * 
 * @author adaml
 *
 */
public class StartMachineThrottler {
	
	private static final int NUMBER_OF_CALLS = 12;

	private static final double START_MACHINE_INVOKATION_LIMIT_PER_SECOND = 10;
	
	private RateLimiter throttler = RateLimiter.create(START_MACHINE_INVOKATION_LIMIT_PER_SECOND);
	
	@Test
	public void testThrottler() {
		final long startTime = System.currentTimeMillis();
		for (int i = 0; i < NUMBER_OF_CALLS; i++) {
			startMachine();
		}
		final long endTime = System.currentTimeMillis() - startTime;
		boolean throttled = TimeUnit.MILLISECONDS.toSeconds(endTime) >= 1;
		Assert.assertTrue("'for' loop ended in less then a second. throttling didn't work.", throttled);
	}
	
	private void startMachine() {
		throttler.acquire();
	}
}
