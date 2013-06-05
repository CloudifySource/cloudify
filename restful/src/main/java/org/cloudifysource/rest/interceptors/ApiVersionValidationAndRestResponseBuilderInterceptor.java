package org.cloudifysource.rest.interceptors;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
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

    private static final String CURRENT_API_VERSION = PlatformVersion.getVersion();

    @Autowired(required = true)
    private MessageSource messageSource;


    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response, final Object handler)
            throws Exception {

        String requestVersion = extractVersionFromRequest(request);
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

        Object model = filterModel(modelAndView.getModel());
        modelAndView.clear();
        response.setContentType(MediaType.APPLICATION_JSON);
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

    // returns the actual model returned by the contorller.
    // this implementation assumes the model consists of just one object and a BindingResult.
    // TODO eli - check how to make a more robust filter.
    private Object filterModel(final Map<String, Object> model) {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof BindingResult)) {
                return value;
            }
        }
        return null;
    }
}
