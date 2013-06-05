/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
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
 * <li>200 OK – if action is successful</li> <li>4** - In case of permission
 * problem or illegal URL</li> <li>5** - In case of exception or server error</li>
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

    @Autowired(required = true)
    protected MessageSource messageSource;

    /**
     * Handles expected exception from the controller, and wrappes it nicely
     * with a {@link Response} object.
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
        Object[] messageArgs = (Object[]) e.getErrorDescription().get(
                "error_args");
        String formattedMessage = messageSource.getMessage(messageId,
                messageArgs, Locale.US);

        Response<Void> finalResponse = new Response<Void>();
        finalResponse.setStatus("Failed");
        finalResponse.setMessage(formattedMessage);
        finalResponse.setMessageId(messageId);
        finalResponse.setResponse(null);
        finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

        String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
        response.getOutputStream().write(responseString.getBytes());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(final HttpServletResponse response,
                                                final ResourceNotFoundException e) throws IOException {

        String messageId = CloudifyMessageKeys.MISSING_RESOURCE.getName();
        Object[] messageArgs = new Object[] {e.getResourceDescription()};
        String formattedMessage = messageSource.getMessage(messageId,
                messageArgs, Locale.US);

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
     * Handles unexpected exceptions from the controller, and wrappes it nicely
     * with a {@link Response} object.
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

        Response<Void> finalResponse = new Response<Void>();
        finalResponse.setStatus("Failed");
        finalResponse.setMessage(t.getMessage());
        finalResponse.setMessageId(CloudifyErrorMessages.GENERAL_SERVER_ERROR
                .getName());
        finalResponse.setResponse(null);
        finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(t));

        String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
        response.getOutputStream().write(responseString.getBytes());
    }
}
