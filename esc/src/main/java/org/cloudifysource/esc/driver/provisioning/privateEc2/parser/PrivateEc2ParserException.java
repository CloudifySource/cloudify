/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2.parser;

/**
 * Exception class for CFN parser.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class PrivateEc2ParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public PrivateEc2ParserException() {
	}

	public PrivateEc2ParserException(final String message) {
		super(message);
	}

	public PrivateEc2ParserException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PrivateEc2ParserException(final Throwable cause) {
		super(cause);
	}

}
