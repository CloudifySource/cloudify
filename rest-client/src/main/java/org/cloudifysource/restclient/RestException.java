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
 ******************************************************************************/
package org.cloudifysource.restclient;

/**
 * Exception representing a failure in the communication through rest. For More
 * detailed exceptions use
 * {@link org.cloudifysource.restclient.ErrorStatusException}
 */
public class RestException extends Exception {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = -7304916239245226345L;

	/**
	 * Empty Ctor.
	 */
	public RestException() {
	}

	/**
	 * @param message
	 *            A String error message describing this exception
	 */
	public RestException(final String message) {
		super(message);

	}

	/**
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 */
	public RestException(final Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 *            A String error message describing this exception
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 */
	public RestException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
