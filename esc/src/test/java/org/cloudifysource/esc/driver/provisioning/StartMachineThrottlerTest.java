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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/**
 * test start machine rate limiter.
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public class StartMachineThrottlerTest {
	
	private static final int NUM_RETRIES = 5;
	// changing this param will segnificantly increase the duration of the test
	private static final int TOTAL_NUMBER_OF_CALLS = 15;
	private static final int COOL_DOWN_PERIOD_SECONDS = 1;
	
	private RequestRateLimiter requestRateLimiter; 
	
	@Test
	public void testThrottler() {
		requestRateLimiter = new RequestRateLimiter(NUM_RETRIES, COOL_DOWN_PERIOD_SECONDS, TimeUnit.SECONDS);
		requestRateLimiter.init();
		final long startTime = System.currentTimeMillis();
		for (int i = 0; i < TOTAL_NUMBER_OF_CALLS; i++) {
			requestRateLimiter.block();
			// on each time we get to the limit, we test the time passed.
			if ((i % NUM_RETRIES == 0) && (i != 0)) {
				assertBlockWasPerformed(i / NUM_RETRIES, startTime);
			}
		}
	}

	private void assertBlockWasPerformed(final int blockAttempts, final long startTime) {
		final long duration = System.currentTimeMillis() - startTime;
		final long durationSec = TimeUnit.MILLISECONDS.toSeconds(duration);
		Assert.assertTrue("Rate limit was not enforced.",
				durationSec >= (COOL_DOWN_PERIOD_SECONDS * blockAttempts) 
				&& durationSec <= ((COOL_DOWN_PERIOD_SECONDS + 1) * blockAttempts));
	}
	
	@Test
	public void assertCloudParams() throws IOException, DSLException {
		final File cloudFile = new File("src/test/resources/cloud_driver/ec2-cloud.groovy");
		final Cloud cloud = ServiceReader.readCloud(cloudFile);
		final Boolean enabled = (Boolean) cloud.getCustom()
							.get(CloudifyConstants.CUSTOM_PROPERTY_START_MACHINE_THROTTLING_ENABLED);
		final Long timeFrame = (Long) cloud.getCustom()
							.get(CloudifyConstants.CUSTOM_PROPERTY_START_MACHINE_THROTTLING_TIME_FRAME_SEC); 
		final Integer requests = (Integer) cloud.getCustom()
				.get(CloudifyConstants.CUSTOM_PROPERTY_START_MACHINE_THROTTLING_NUM_REQUESTS); 
		
		Assert.assertTrue(enabled);
		Assert.assertTrue(timeFrame == 300);
		Assert.assertTrue(requests == 5);
		
	}
}
