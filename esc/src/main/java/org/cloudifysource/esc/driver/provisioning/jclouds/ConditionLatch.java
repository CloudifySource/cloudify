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
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;


public class ConditionLatch {

	private static final long DEFAULT_INTERVAL_SECONDS = 10;
	private static final String DEFAULT_TIMEOUT_ERROR_MESSAGE = "Operation timed out";
	
	private final static Logger logger = Logger.getLogger(ConditionLatch.class.getName());
	private String timeoutErrorMessage = DEFAULT_TIMEOUT_ERROR_MESSAGE;
	private long pollingIntervalMilliseconds = TimeUnit.SECONDS.toMillis(DEFAULT_INTERVAL_SECONDS);
	private boolean verbose = false;
	private long timeoutMilliseconds;
	
	public interface Predicate {
    	boolean isDone() throws CloudProvisioningException, InterruptedException;
	}
	
	public ConditionLatch timeoutErrorMessage(String timeoutErrorMessage) {
		this.timeoutErrorMessage = timeoutErrorMessage;
		return this;
	}
	
	public ConditionLatch pollingInterval(long duration, TimeUnit timeunit) {
		this.pollingIntervalMilliseconds = timeunit.toMillis(duration);
		return this;
	}
	
	public ConditionLatch verbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}
	
	public ConditionLatch timeout(long timeout, TimeUnit timeunit) {
		this.timeoutMilliseconds = timeunit.toMillis(timeout);
		return this;
	}
	
	public void waitFor(Predicate predicate) throws InterruptedException, TimeoutException, CloudProvisioningException {
	
		long end = System.currentTimeMillis() + timeoutMilliseconds;
	    
	    boolean isDone = predicate.isDone();
	    while(!isDone && System.currentTimeMillis() < end) {
	    	if (verbose) {
	    		logger.log(Level.INFO, 
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
