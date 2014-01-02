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
 ******************************************************************************/
package org.cloudifysource.rest.interceptors;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.j_spaces.kernel.PlatformVersion;

/**
 * This intercepter has two goals.
 * <br><br>
 * 1. Validate the request is made with the current API version of the REST Gateway.
 * <br>
 * 2. Construct the {@link Response} Object after the controller has finished handling the request.
 * @author elip
 *
 */
public class ApiVersionValidationAndRestResponseBuilderInterceptor extends HandlerInterceptorAdapter {

	private static final Logger logger = Logger
			.getLogger(ApiVersionValidationAndRestResponseBuilderInterceptor.class.getName());
	
    private static final String CURRENT_API_VERSION = PlatformVersion.getVersion();

    @Autowired(required = true)
    private MessageSource messageSource;


    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response, final Object handler)
            throws Exception {
    	String requestVersion = extractVersionFromRequest(request);
    	if (CloudifyConstants.SERVICE_CONTROLLER_URL.equals(requestVersion) 
    			|| CloudifyConstants.ADMIN_API_CONTROLLER_URL.equals(requestVersion)) {
    		// if this is a request to service/** or to admin/** than this is not the right interceptor.
    		// (for example service/templates request will arrive here because of the 'templates' suffix 
    		// but it is not a template controller and it will be processed in the VersionValidateInterceptor)
    		return true;
    	}
    	if (logger.isLoggable(Level.FINEST)) {
    		logger.finest("pre handle request from " + request.getRequestURI());
    	}
        // checks the request's version
        if (!CURRENT_API_VERSION.equalsIgnoreCase(requestVersion)) {
            throw new RestErrorException(CloudifyMessageKeys.API_VERSION_MISMATCH.getName(),
                    requestVersion, CURRENT_API_VERSION);
        }

        return true;
    }

    private String extractVersionFromRequest(final HttpServletRequest request) {
        String requestURIWithoutContextPath =
                request.getRequestURI().substring(request.getContextPath().length()).substring(1);
        return requestURIWithoutContextPath.split("/")[0];
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response, final Object handler,
                           final ModelAndView modelAndView) throws Exception {
    	String requestVersion = extractVersionFromRequest(request);
    	if (CloudifyConstants.SERVICE_CONTROLLER_URL.equals(requestVersion)) {
    		// if this is a request to service/** than this is not the right interceptor.
    		// (for example service/templates request will arrive here because of the 'templates' suffix 
    		// but it is not a template controller and it will be processed in the VersionValidateInterceptor)
    		return;
    	}
    	
    	if (logger.isLoggable(Level.FINEST)) {
    		logCurrentStatus(request, modelAndView);
    	}
    	
        Object model = filterModel(modelAndView, handler);
        modelAndView.clear();
        response.setContentType(CloudifyConstants.MIME_TYPE_APPLICATION_JSON);
        if (model instanceof Response<?>) {
            String responseBodyStr = new ObjectMapper().writeValueAsString(model);
            response.getOutputStream().write(responseBodyStr.getBytes());
            response.getOutputStream().close();

        } else {
            Response<Object> responseBodyObj = new Response<Object>();
            responseBodyObj.setResponse(model);
            responseBodyObj.setStatus("Success");
            responseBodyObj.setMessage(messageSource.getMessage(CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName(),
                    new Object[] {}, Locale.US));
            responseBodyObj.setMessageId(CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName());
            String responseBodyStr = new ObjectMapper().writeValueAsString(responseBodyObj);
            response.getOutputStream().write(responseBodyStr.getBytes());
            response.getOutputStream().close();
        }

    }

	private void logCurrentStatus(final HttpServletRequest request, final ModelAndView modelAndView) {
		String requestUri = request.getRequestURI();
		Map<String, Object> model = modelAndView.getModel();
		View view = modelAndView.getView();
		
		StringBuilder message = new StringBuilder("post handle request");
		if (requestUri == null) {
			message.append(", requestUri is null");
		} else {
			message.append(" from " + request.getRequestURI());
		}

		if (model == null) {
			message.append(", model is null");
		} else {
			message.append(" with model " + model.toString());
		}
		
		if (view == null) {
			message.append(", view is null");
		} else {
			message.append(" and view " + view.toString());
		}
		
		logger.finest(message.toString());
	}


    /**
     * Filters the modelAndView object and retrieves the actual object returned by the controller.
     * This implementation assumes the model consists of just one returned object and a BindingResult.
     * If the model is empty, the supported return types are String (the view name) or void.
     */
    private Object filterModel(final ModelAndView modelAndView, final Object handler) 
    	throws RestErrorException {
    	
    	Object methodReturnObject = null;
    	Map<String, Object> model = modelAndView.getModel();
    	

    	if (model != null && !model.isEmpty()) {

    		// the model is not empty. The return value is the first value that is not a BindingResult
    		for (Map.Entry<String, Object> entry : model.entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof BindingResult)) {
                	methodReturnObject = value;
                	break;
                }
            }

    	} else {
    		// the model is empty, this means the return type is String or void
    		if (handler instanceof HandlerMethod) {
        		Class<?> returnType = ((HandlerMethod) handler).getMethod().getReturnType();
        		if (returnType == Void.TYPE) {
        			methodReturnObject = null;
        		} else if (returnType == String.class) {
        			String viewName = modelAndView.getViewName();
        			methodReturnObject = viewName;
        		} else {
        			logger.warning("return type not supported: " + returnType);
        			throw new RestErrorException("return type not supported: " + returnType);
        		}
        	} else {
        		logger.warning("handler object is not a HandlerMethod: " + handler);
    			throw new RestErrorException("handler object is not a HandlerMethod: " + handler);
        	}
    	}
    	
        return methodReturnObject;
    }
}
