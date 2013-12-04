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
 *******************************************************************************/
package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.response.AddTemplatesStatus;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * Provides methods usefully for implementation Rest Controller <br>
 * </br> e.g. <br>
 * </br> getApplication(appName) get application by given application name
 * 
 * <ul>
 * <h3>possible response codes</h3>
 * </ul>
 * <li>200 OK – if action is successful</li> <li>4** - In case of permission problem or illegal URL</li> <li>5** - In
 * case of exception or server error</li>
 * 
 * @throws UnsupportedOperationException
 *             , org.cloudifysource.rest.controllers.RestErrorException
 * 
 * 
 * 
 *             <h3>Note :</h3>
 *             <ul>
 *             this class must be thread safe
 *             </ul>
 * 
 * @author ahmad
 * @since 2.5.0
 */

public abstract class BaseRestController {

	// thread safe
	// @see http://wiki.fasterxml.com/JacksonFAQ for more info.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger logger = Logger.getLogger(BaseRestController.class.getName());

	@Autowired(required = true)
	protected MessageSource messageSource;

	/**
	 * Handles expected exception from the controller, and wrappes it nicely with a {@link Response} object.
	 * 
	 * @param response
	 *            - the servlet response.
	 * @param e
	 *            - the thrown exception.
	 * @throws IOException .
	 */
	@ExceptionHandler(AddTemplatesException.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void handleAddTemplatesException(final HttpServletResponse response,
			final AddTemplatesException e) throws IOException {

		AddTemplatesStatus status = e.getAddTemplatesResponse().getStatus();
		String messageId = CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName();
		if (status == AddTemplatesStatus.PARTIAL_FAILURE) {
			messageId = CloudifyErrorMessages.PARTLY_FAILED_TO_ADD_TEMPLATES.getName();
		}
		String formattedMessage;
		try {
			formattedMessage = messageSource.getMessage(messageId, null, Locale.US);
		} catch (NoSuchMessageException ne) {
			formattedMessage = messageId;
		}

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus(status.getName());
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		String addTemplatesResponseAsString = OBJECT_MAPPER.writeValueAsString(e.getAddTemplatesResponse());
		Logger.getLogger(BaseRestController.class.getName())
				.log(Level.INFO,
						"[handleAddTemplatesException] - create failed status response with verbose: "
								+ addTemplatesResponseAsString);
		finalResponse.setVerbose(addTemplatesResponseAsString);

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * Handles expected exception from the controller, and wrappes it nicely with a {@link Response} object.
	 * 
	 * @param response
	 *            - the servlet response.
	 * @param e
	 *            - the thrown exception.
	 * @throws IOException .
	 */
	@ExceptionHandler(RestErrorException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public void handleExpectedErrors(final HttpServletResponse response,
			final RestErrorException e) throws IOException {

		String messageId = (String) e.getErrorDescription().get("error");
		Object[] messageArgs = (Object[]) e.getErrorDescription().get("error_args");
		String formattedMessage;
		try {
			formattedMessage = messageSource.getMessage(messageId, messageArgs, Locale.US);
		} catch (NoSuchMessageException ne) {
			String args = Arrays.toString(messageArgs);
			if (logger.isLoggable(Level.WARNING)) {
				logger.warning("[handleResourceNotFoundException] - failed to get message from messageSource ["
						+ "messageId " + messageId + " arguments " + args + "]");
			}
			formattedMessage = messageId + (args == null ? "" : " [" + args + "]");
		}

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * Handles expected access denied exception from the controller, and wrappes it nicely with a {@link Response}
	 * object.
	 * 
	 * @param response
	 *            - the servlet response.
	 * @param e
	 *            - the thrown exception.
	 * @throws IOException .
	 */
	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public void handleAccessDeniedErrors(final HttpServletResponse response,
			final AccessDeniedException e) throws IOException {

		String messageId = CloudifyErrorMessages.NO_PERMISSION_ACCESS_DENIED.getName();
		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(messageId + " [" + e.getMessage() + "]");
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * 
	 * @param response
	 * @param e
	 * @throws IOException
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public void handleResourceNotFoundException(final HttpServletResponse response,
			final ResourceNotFoundException e) throws IOException {

		String messageId = CloudifyMessageKeys.MISSING_RESOURCE.getName();
		Object[] messageArgs = new Object[] { e.getResourceDescription() };
		String formattedMessage;
		try {
			formattedMessage = messageSource.getMessage(messageId, messageArgs, Locale.US);
		} catch (NoSuchMessageException ne) {
			String args = Arrays.toString(messageArgs);
			if (logger.isLoggable(Level.WARNING)) {
				logger.warning("[handleResourceNotFoundException] - failed to get message from messageSource ["
						+ "messageId: " + messageId + " arguments: " + args + "]");
			}
			formattedMessage = messageId + (args == null ? "" : " [" + args + "]");
		}

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("[handleResourceNotFoundException] - update failed response [message "
					+ formattedMessage + " message ID " + messageId + "]");
		}

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * Handles unexpected exceptions from the controller, and wrappes it nicely with a {@link Response} object.
	 * 
	 * @param response
	 *            - the servlet response.
	 * @param t
	 *            - the thrown exception.
	 * @throws IOException .
	 */
	@ExceptionHandler(Throwable.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void handleUnExpectedErrors(final HttpServletResponse response,
			final Throwable t) throws IOException {

		Object[] messageArgs = new Object[] { t.getMessage() };
		String formattedMessage =
				messageSource.getMessage(
						CloudifyErrorMessages.GENERAL_SERVER_ERROR.getName(),
						messageArgs,
						Locale.US);

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(CloudifyErrorMessages.GENERAL_SERVER_ERROR.getName());
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(t));

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * Handles unsupported operation exception from the controller, and wrappes it nicely with a {@link Response}
	 * object.
	 * 
	 * @param response
	 *            the servlet response.
	 * @param e
	 *            the thrown exception.
	 * @throws IOException
	 *             If failed to write the response.
	 */
	@ExceptionHandler(UnsupportedOperationException.class)
	@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
	public void handleUnsupportedOperationException(final HttpServletResponse response,
			final UnsupportedOperationException e) throws IOException {
		String messageId = CloudifyErrorMessages.UNSUPPORTED_OPERATION.getName();
		String formattedMessage;
		try {
			Object[] args = { e.getMessage() };
			formattedMessage = messageSource.getMessage(messageId, args, Locale.US);
		} catch (NoSuchMessageException ne) {
			formattedMessage = messageId;
		}

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

		String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}
}
