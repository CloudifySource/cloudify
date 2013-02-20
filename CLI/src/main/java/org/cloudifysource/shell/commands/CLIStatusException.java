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

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restclient.ErrorStatusException;

/**
 * @author noak
 * @since 2.0.0
 * 
 *        Extends {@link CLIException}, includes more details to support
 *        formatted messages.
 */
public class CLIStatusException extends CLIException {

	private static final long serialVersionUID = -399277091070772297L;
	private final String reasonCode;
	private final Object[] args;

	private final String verboseData;

	public CLIStatusException(final ErrorStatusException restException) {
		super("reasonCode: " + restException.getReasonCode(), restException);
		this.args = restException.getArgs();
		this.reasonCode = restException.getReasonCode();
		this.verboseData = restException.getVerboseData();
	}

	/**
	 * Constructor.
	 * 
	 * @param cause
	 *            The Throwable that caused this exception to be thrown.
	 * @param reasonCode
	 *            A reason code, by which a formatted message can be retrieved
	 *            from the message bundle
	 * @param args
	 *            Optional arguments to embed in the formatted message
	 */
	public CLIStatusException(final Throwable cause, final String reasonCode, final Object... args) {
		super("reasonCode: " + reasonCode, cause);
		this.args = args;
		this.reasonCode = reasonCode;
		this.verboseData = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param reasonCode
	 *            A reason code, by which a formatted message can be retrieved
	 *            from the message bundle
	 * @param args
	 *            Optional arguments to embed in the formatted message
	 */
	public CLIStatusException(final String reasonCode, final Object... args) {
		super("reasonCode: " + reasonCode);
		this.reasonCode = reasonCode;
		this.args = args;
		this.verboseData = null;
	}

	/**
	 * Gets the reason code.
	 * 
	 * @return A reason code, by which a formatted message can be retrieved from
	 *         the message bundle
	 */
	public String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets the arguments that complete the reason-code based message.
	 * 
	 * @return An array of arguments
	 */
	public Object[] getArgs() {
		return args;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "CLIStatusException, reason code: " + reasonCode + ", message arguments: "
				+ StringUtils.join(args, ", ");
	}

	public String getVerboseData() {
		return verboseData;
	}
}
