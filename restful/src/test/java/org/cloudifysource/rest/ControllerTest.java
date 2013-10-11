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
package org.cloudifysource.rest;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * A class for testing the controllers.
 * @author yael
 *
 */
public abstract class ControllerTest {
    private static final ObjectMapper PROJECT_MAPPER = new ObjectMapper();

    protected RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Given a uri and a request method, gets the expected handler method.
     * @param requestUri - the uri.
     * @param requestMethod - {@link RequestMethod#GET}, {@link RequestMethod#POST} or {@link RequestMethod#DELETE}
     * @return the expected handler method.
     */
    public abstract HandlerMethod getExpectedMethod(final String requestUri, final RequestMethod requestMethod);

    @Before
    public void initControllerTest() throws NoSuchMethodException {
        handlerAdapter = applicationContext.getBean(RequestMappingHandlerAdapter.class);
    }

    public void testGet(final String requestUri, final String expectedResponseContent) throws Exception {
        final MockHttpServletRequest reqeust = createMockGetRequest(requestUri);
        testRequest(reqeust, getExpectedMethod(requestUri, RequestMethod.GET), expectedResponseContent);
    }

    public void testDelete(final String requestUri, final String expectedResponseContent) throws Exception {
        final MockHttpServletRequest reqeust = createMockDeleteRequest(requestUri);
        testRequest(reqeust, getExpectedMethod(requestUri, RequestMethod.DELETE), expectedResponseContent);
    }

    public void testPost(final String requestUri, final String postContentAsJson, final String expectedResponseContent)
            throws Exception {
        final MockHttpServletRequest reqeust = createMockPostRequest(requestUri, postContentAsJson);
        testRequest(reqeust, getExpectedMethod(requestUri, RequestMethod.POST), expectedResponseContent);
    }

    public MockHttpServletResponse testPostFile(final String requestUri, final File file)
            throws Exception {
        final MockHttpServletRequest reqeust = createMockPostFileRequest(requestUri, file);
        return testRequest(reqeust, getExpectedMethod(requestUri, RequestMethod.POST));
    }

    private MockHttpServletResponse testRequest(final MockHttpServletRequest request,
                                                final HandlerMethod expectedHandlerMethod) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final HandlerExecutionChain handlerExecutionChain = getHandlerToRequest(request);
        Object handler = handlerExecutionChain.getHandler();
        Assert.assertEquals("Wrong handler selected for request uri: "
                + request.getRequestURI(), expectedHandlerMethod.toString(),
                handler.toString());

        HandlerInterceptor[] interceptors = handlerExecutionChain.getInterceptors();
        // pre handle
        for (HandlerInterceptor handlerInterceptor : interceptors) {
            handlerInterceptor.preHandle(request, response, handler);
        }
        // handle the request
        ModelAndView modelAndView = handlerAdapter.handle(request, response, handler);
        // post handle
        for (HandlerInterceptor handlerInterceptor : interceptors) {
            handlerInterceptor.postHandle(request, response, handler, modelAndView);
        }

        // validate the response
        Assert.assertTrue("Wrong response status: " + response.getStatus(),
                response.getStatus() == HttpStatus.OK.value());
        Assert.assertTrue(response.getContentType().contains(CloudifyConstants.MIME_TYPE_APPLICATION_JSON));

        return response;
    }

    private void testRequest(final MockHttpServletRequest reqeust,
                             final HandlerMethod expectedHandlerMethod,
                             final String expectedResponseContent) throws Exception {

        final MockHttpServletResponse response = testRequest(reqeust, expectedHandlerMethod);

        // validate the response's content
        Assert.assertEquals(
                "Wrong response content: " + response.getContentAsString(),
                expectedResponseContent, response.getContentAsString());
    }

    /**
     * This method finds the handler for a given request URI. It will also ensure that the URI Parameters i.e.
     * /context/test/{name} are added to the request
     *
     * @param request
     *            The request object to be used
     * @return The correct handler for the request
     * @throws Exception
     *             Indicates a matching handler could not be found
     */
    private HandlerExecutionChain getHandlerToRequest(final MockHttpServletRequest request)
            throws Exception {
        HandlerExecutionChain chain = null;

        final Map<String, HandlerMapping> map = applicationContext
                .getBeansOfType(HandlerMapping.class);

        for (final HandlerMapping mapping : map.values()) {
            chain = mapping.getHandler(request);

            if (chain != null) {
                break;
            }
        }

        if (chain == null) {
            throw new InvalidParameterException(
                    "Unable to find handler for request URI: "
                            + request.getRequestURI());
        }
        return chain;
    }

    private MockHttpServletRequest createMockGetRequest(final String requestUri) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        request.setMethod("GET");
        request.setContentType(CloudifyConstants.MIME_TYPE_APPLICATION_JSON);

        return request;
    }

    private MockHttpServletRequest createMockPostRequest(
            final String requestUri, final String contentAsJson) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        request.setMethod("POST");
        request.setContentType(CloudifyConstants.MIME_TYPE_APPLICATION_JSON);

        if (StringUtils.isNotBlank(contentAsJson)) {
            request.setContent(contentAsJson.getBytes());
        }

        return request;
    }

    private MockHttpServletRequest createMockPostFileRequest(
            final String requestUri, final File file) throws IOException {
        MultipartFile multiFile = UploadRepoTest.createNewMultiFile(file);
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.addFile(multiFile);
        request.setMethod("POST");
        request.setRequestURI(requestUri);
        return request;
    }

    private MockHttpServletRequest createMockDeleteRequest(
            final String requestUri) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        request.setMethod("DELETE");
        request.setContentType(CloudifyConstants.MIME_TYPE_APPLICATION_JSON);

        return request;
    }

    /**
     * Converts a json String to a Map<String, Object>.
     *
     * @param jsonStr
     *            a json-format String to convert to a map
     * @return a Map<String, Object> based on the given json-format String
     * @throws IOException
     *             Reporting failure to read or convert the json-format string to a map
     */
    public static Map<String, Object> jsonToMap(final String jsonStr)
            throws IOException {
        return PROJECT_MAPPER.readValue(jsonStr, TypeFactory.type(Map.class));
    }

    /**
     * Converts an object to a json-format String.
     *
     * @param value
     *            an object to convert to json-format String
     * @return a json-format String based on the given object
     * @throws IOException
     *             Reporting failure to convert the object
     */
    public static String convertToJson(final Object value) throws IOException {
        return PROJECT_MAPPER.writeValueAsString(value);
    }

}
