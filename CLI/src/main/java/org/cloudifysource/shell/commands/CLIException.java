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
package org.cloudifysource.shell.commands;

/**
 * @author rafi, noak
 * @since 2.0.0
 * 
 *        Represents exceptions in the scope of the command line interface.
 */
public class CLIException extends Exception {

	private static final long serialVersionUID = 1295396747968774683L;

	/**
	 * Empty Construction.
	 */
	public CLIException() {
	}

	/**
	 * Constructor.
	 * @param message A detailed message about the exception
	 */
	public CLIException(final String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param cause The Throwable that caused this exception to be thrown.
	 */
	public CLIException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 * @param message A detailed message about the exception
	 * @param cause The Throwable that caused this exception to be thrown.
	 */
	public CLIException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
