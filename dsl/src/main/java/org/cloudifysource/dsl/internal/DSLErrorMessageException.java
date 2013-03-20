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
package org.cloudifysource.dsl.internal;

/*********
 * A DSL exception that uses the standard error message enum. Clients should format the error message as required.
 * @author barakme
 * @since 2.5.0
 *
 */
public class DSLErrorMessageException extends DSLException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final CloudifyErrorMessages errorMessage;
	private final String[] args;

	public DSLErrorMessageException(final CloudifyErrorMessages errorMessage, final String... args) {
		super(errorMessage.getName());
		this.errorMessage = errorMessage;
		this.args = args;
	}



	public String[] getArgs() {
		return args;
	}



	public CloudifyErrorMessages getErrorMessage() {
		return errorMessage;
	}
}
