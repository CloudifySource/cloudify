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
package org.cloudifysource.restclient.messages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientHttpException;
import org.cloudifysource.restclient.exceptions.RestClientIOException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;

/**
 * Handles the RestClient's messages.
 * 
 * @author yael
 * 
 */
public final class MessagesUtils {

	private static final Logger logger = Logger.getLogger(MessagesUtils.class.getName());

	/**
	 * The message bundle that holds the message.
	 */
	private static ResourceBundle messageBundle;

	private MessagesUtils() {

	}

	/**
	 * returns the message as it appears in the {@link #messageBundle}.
	 * 
	 * @param messageCode
	 *            The message key as it is defined in the message bundle.
	 * @param arguments
	 *            The message arguments
	 * @return the formatted message according to the message key.
	 */
	public static String getFormattedMessage(final String messageCode, final Object... arguments) {

		final String message = getMessageBundle().getString(messageCode);
		if (message == null) {
			logger.warning("Missing resource in messages resource bundle: " + messageCode);
			return messageCode;
		}
		try {
			return MessageFormat.format(message, arguments);
		} catch (final IllegalArgumentException e) {
			logger.warning("Failed to format message: " + messageCode + " with format: "
					+ message + " and arguments: " + Arrays.toString(arguments));
			return messageCode;
		}
	}
	
	/**
	 * returns the message as it appears in the {@link #messageBundle}.
	 * No warning is issued if the message is not found.
	 * 
	 * @param messageCode
	 *            The message key as it is defined in the message bundle.
	 * @param arguments
	 *            The message arguments
	 * @return the formatted message according to the message key.
	 */
	public static String getFormattedMessageSilently(final String messageCode, final Object... arguments) {

		// the message defaults to the message code
		String formattedMessage = messageCode;
		final ResourceBundle messageBundle = getMessageBundle();
		if (messageBundle.containsKey(messageCode)) {
			String bundleMessage = messageBundle.getString(messageCode);
			if (StringUtils.isNotBlank(bundleMessage)) {
				try {
					formattedMessage = MessageFormat.format(bundleMessage, arguments);
				} catch (final IllegalArgumentException e) {
					// ignore, we will return the message code instead
				}
			}
		}
		
		return formattedMessage;
		
	}

	private static ResourceBundle getMessageBundle() {
		if (messageBundle == null) {
			messageBundle = ResourceBundle.getBundle("MessagesBundle_RestClient", Locale.getDefault());
		}
		return messageBundle;
	}

	/**
	 * Creates an RestClientException with given messageCode and the formatted message with the given arguments (using
	 * the {@link #messageBundle}).
	 * 
	 * @param messageCode
	 *            The message key as it is defined in the message bundle.
	 * @param arguments
	 *            The message arguments
	 * @return a new RestClientException.
	 */
	public static RestClientException createRestClientException(
			final String messageCode,
			final Object... arguments) {
		return new RestClientException(
				messageCode,
				getFormattedMessage(
						messageCode,
						arguments),
				null);
	}

	/**
	 * Creates an RestClientIOException with given messageCode, the formatted message with the given arguments (using
	 * the {@link #messageBundle}) and the given exception.
	 * 
	 * @param messageCode
	 *            The message key as it is defined in the message bundle.
	 * @param exception
	 *            The IOException
	 * @param arguments
	 *            The message arguments
	 * @return a new RestClientIOException.
	 */
	public static RestClientIOException createRestClientIOException(
			final String messageCode,
			final IOException exception,
			final Object... arguments) {
		return new RestClientIOException(
				messageCode,
				getFormattedMessage(messageCode, arguments),
				exception);
	}
	
	/**
	 * Creates a RestClientResponseException with given messageCode, 
	 * the formatted message with the given arguments (using
	 * the {@link #messageBundle}), the status code, reason
	 * and response body.
	 * 
	 * @param statusCode the status code of the response.
	 * @param reasonPhrase the reasonPhrase of the response.
	 * @param responseBody the body of the response.
	 * @param defaultMessage the message to use if a message is not found in the message bundle.
	 * @param messageCode the message key as it is defined in the message bundle.
	 * @param arguments the message arguments
	 * @return a new RestClientResponseException.
	 */
	public static RestClientResponseException createRestClientResponseException(
			final int statusCode, 
			final String reasonPhrase,
			final String responseBody,
			final String defaultMessage,
			final String messageCode,
			final Object... arguments) {
		String  formattedMessage = getFormattedMessageSilently(messageCode, arguments);
		if (formattedMessage.equalsIgnoreCase(messageCode) && StringUtils.isNotBlank(defaultMessage)) {
			formattedMessage = defaultMessage;
		}
		return new RestClientResponseException(
				messageCode,
				formattedMessage,
				statusCode,
				reasonPhrase,
				responseBody);
	}

	/**
	 * Creates a RestClientHttpException with given messageCode, 
	 * the formatted message with the given arguments (using
	 * the {@link #messageBundle}), the given exception, 
	 * response body and status line details.
	 * 
	 * @param messageCode
	 *            The message key as it is defined in the message bundle.
	 * @param exception
	 *            The IOException
	 * @param arguments
	 *            The message arguments
	 * @param statusCode the status code of the response.
	 * @param reasonPhrase the reasonPhrase of the response.
	 * @param responseBody the body of the response.
	 * @return a new RestClientHttpException.
	 */
	public static RestClientHttpException createRestClientHttpException(
			final IOException exception,
			final int statusCode, 
			final String reasonPhrase,
			final String responseBody,
			final String messageCode,
			final Object... arguments) {
		return new RestClientHttpException(
				messageCode,
				getFormattedMessage(messageCode, arguments),
				statusCode,
				reasonPhrase,
				responseBody,
				exception);
	}

}
