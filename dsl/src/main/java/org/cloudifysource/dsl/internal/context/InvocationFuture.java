/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.dsl.internal.context;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;

/****************
 * A wrapper for the future returned by a custom command invocation. It handles the processing of the map returned by an
 * invocation. Most methods are handled by delegating to the original future, the main logic is in
 * handleInvocationResponse().
 * 
 * @author barakme
 * 
 */
class InvocationFuture implements Future<Object> {

	private final Future<Object> delegate;

	public InvocationFuture(final Future<Object> future) {
		this.delegate = future;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return this.delegate.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.delegate.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.delegate.isDone();
	}

	@Override
	public Object get() throws InterruptedException, ExecutionException {
		final Object result = this.delegate.get();

		return handleInvocationResponse(result);

	}

	private Object handleInvocationResponse(final Object result) throws ExecutionException {
		@SuppressWarnings("unchecked")
		final Map<String, Object> map = (Map<String, Object>) result;
		final Boolean success = (Boolean) map.get(CloudifyConstants.INVOCATION_RESPONSE_STATUS);
		if (success == null) {
			throw new IllegalStateException("Was expecting: " + CloudifyConstants.INVOCATION_RESPONSE_STATUS
					+ " field in invocation response");
		}

		if (success) {
			return map.get(CloudifyConstants.INVOCATION_RESPONSE_RESULT);
		} else {
			final Exception e = (Exception) map.get(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION);
			throw new ExecutionException(e);
		}
	}

	@Override
	public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException {
		final Object result = this.delegate.get(timeout, unit);
		return handleInvocationResponse(result);
	}

}
