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

package org.cloudifysource.dsl.internal;

/****************
 * Result of the compilation of an external groovy file, indicating if the file is a valid groovy script file.
 * 
 * @author barakme
 * @since 2.2
 * 
 */
public class GroovyFileCompilationResult {

	private final boolean success;

	private final String compilationErrorMessage;
	private final String errorMessage;

	private final Throwable cause;
	/*******
	 * A constant for success results.
	 */
	public static final GroovyFileCompilationResult SUCCESS = new GroovyFileCompilationResult(true, null, null, null);

	public GroovyFileCompilationResult(final boolean success, final String compilationErrorMessage,
			final String errorMessage, final Throwable cause) {
		super();
		this.success = success;
		this.compilationErrorMessage = compilationErrorMessage;
		this.errorMessage = errorMessage;
		this.cause = cause;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getCompilationErrorMessage() {
		return compilationErrorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return "GroovyFileCompilationResult [success=" + success + ", compilationErrorMessage="
				+ compilationErrorMessage + ", errorMessage=" + errorMessage + "]";
	}

	public Throwable getCause() {
		return cause;
	}

}
