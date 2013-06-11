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

import org.apache.commons.lang.StringUtils;

/**
 * This exception extends {@link org.cloudifysource.restclient.RestException} to
 * include a detailed error status. The reasonCode and args array can be used to
 * created formatted messages from the message bundle.
 * 
 * @author uri
 */
public class ErrorStatusException extends RestException {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = -399277091070772297L;

	/**
	 * reason code constant text.
	 */
	private static final String REASON_CODE = "reasonCode";

	/**
	 * unique reason code describing this exception. Matches a formatted message
	 * in the messages bundle.
	 */
	private final String reasonCode;

	/**
	 * an array of possible arguments to complete the reason code.
	 */
	private final Object[] args;

	private String verboseData;

	/**
	 * @param cause
	 *            A Throwable object, the cause of this exception
	 * @param reasonCode
	 *            A unique code used to retrieve the formatted message from a
	 *            message bundle
	 * @param args
	 *            Optional arguments combined in the message
	 */
	public ErrorStatusException(final Throwable cause, final String reasonCode, final Object... args) {
		super(REASON_CODE + ":" + reasonCode, cause);
		this.args = args;
		this.reasonCode = reasonCode;
	}

	/**
	 * @param reasonCode
	 *            A unique code used to retrieve the formatted message from a
	 *            message bundle
	 * @param args
	 *            Optional arguments combined in the message
	 */
	public ErrorStatusException(final String reasonCode, final Object... args) {
		super(REASON_CODE + reasonCode);
		this.reasonCode = reasonCode;
		this.args = args;
	}

	/**
	 * @param reasonCode
	 *            A unique code used to retrieve the formatted message from a
	 *            message bundle
	 * @param verboseData
	 *            verbose data
	 * @param args
	 *            Optional arguments combined in the message
	 */
	public ErrorStatusException(final String reasonCode, final String verboseData, final Object... args) {
		super(REASON_CODE + reasonCode);
		this.reasonCode = reasonCode;
		this.args = args;
		this.verboseData = verboseData;
	}

	/**
	 * Gets the reason code.
	 * 
	 * @return reason code as String
	 */
	public final String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets the arguments array.
	 * 
	 * @return arguments as Object[]
	 */
	public final Object[] getArgs() {
		return args;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "ErrorStatusException, reason code: " + reasonCode + ", message arguments: "
				+ StringUtils.join(args, ", ") + ", Verbose: " + this.verboseData;
	}

	public String getVerboseData() {
		return verboseData;
	}

	public void setVerboseData(final String verboseData) {
		this.verboseData = verboseData;
	}

}