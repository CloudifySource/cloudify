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
 ******************************************************************************/
package org.cloudifysource.rest;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.rest.controllers.AttributesController;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class tests different calls (get/post) to the attributes controller web
 * service, over REST. The new spring REST testing framework is being used.
 * 
 * @author noak
 */
// Swap the default JUnit4 with the spring specific SpringJUnit4ClassRunner.
// This will allow spring to inject the application context
@RunWith(SpringJUnit4ClassRunner.class)
// Setup the configuration of the application context and the web mvc layer
@ContextConfiguration({ "classpath:META-INF/spring/applicationContext.xml",
		"classpath:META-INF/spring/webmvc-config-test.xml" })
public class AttributesContollerTests {

	private static final ObjectMapper PROJECT_MAPPER = new ObjectMapper();
	private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
	private static final String APPLICATION_NAME = "petclinic-simple";
	private static final String SERVICE_NAME = "tomcat";
	private static final String INSTANCE_ID = "1";
	private static final String ATTRIBUTE_NAME = "myAttr";
	private static final String HTTP_GET = "GET";
	private static final String HTTP_POST = "POST";
	private static final String HTTP_DELETE = "DELETE";

	@Autowired
	private ApplicationContext applicationContext;

	private RequestMappingHandlerAdapter handlerAdapter;
	private HashMap<String, HashMap<String, HandlerMethod>> singleAttributeUrisMapping;
	private HashMap<String, HashMap<String, HandlerMethod>> multipleAttributesUrisMapping;

	/**
	 * Initialize the basic objects that are used widely in the tests.
	 * 
	 * @throws NoSuchMethodException
	 *             Indicates the defined {@link HandlerMethod} does not exist
	 */
	@Before
	public void init() throws NoSuchMethodException {
		handlerAdapter = applicationContext
				.getBean(RequestMappingHandlerAdapter.class);
		AttributesController controller = applicationContext
				.getBean(AttributesController.class);

		// TODO: this is a temporary solution. need to get the methods list from
		// the handler mapping somehow.
		singleAttributeUrisMapping = new HashMap<String, HashMap<String, HandlerMethod>>();
		multipleAttributesUrisMapping = new HashMap<String, HashMap<String, HandlerMethod>>();

		// global scope, single attribute
		final String singleGlobalAttributeUri = "/attributes/globals/"
				+ ATTRIBUTE_NAME;
		final HashMap<String, HandlerMethod> singleGlobalAttributeHandlers = new HashMap<String, HandlerMethod>();
		singleGlobalAttributeHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getGlobalAttribute", String.class));
		singleGlobalAttributeHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setGlobalAttribute", String.class, Object.class));
		singleGlobalAttributeHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteGlobalAttribute", String.class));

		// global scope, multiple attributes
		final String multipleGlobalAttributesUri = "/attributes/globals";
		final HashMap<String, HandlerMethod> multipleGlobalAttributesHandlers = new HashMap<String, HandlerMethod>();
		multipleGlobalAttributesHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getGlobalAttributes"));
		multipleGlobalAttributesHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setGlobalAttributes", Map.class));
		multipleGlobalAttributesHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteGlobalAttributes"));

		// application scope, single attribute
		final String singleApplicationAttributeUri = "/attributes/applications/"
				+ APPLICATION_NAME + "/" + ATTRIBUTE_NAME;
		final HashMap<String, HandlerMethod> singleApplicationAttributeHandlers = new HashMap<String, HandlerMethod>();
		singleApplicationAttributeHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getApplicationAttribute", String.class,
				String.class));
		singleApplicationAttributeHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setApplicationAttribute", String.class,
				String.class, Object.class));
		singleApplicationAttributeHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteApplicationAttribute", String.class,
				String.class));

		// application scope, multiple attributes
		final String multipleApplicationAttributesUri = "/attributes/applications/"
				+ APPLICATION_NAME;
		final HashMap<String, HandlerMethod> multipleApplicationAttributesHandlers = 
				new HashMap<String, HandlerMethod>();
		multipleApplicationAttributesHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getApplicationAttributes", String.class));
		multipleApplicationAttributesHandlers.put(HTTP_POST,
				new HandlerMethod(controller, "setApplicationAttributes",
						String.class, Map.class));
		multipleApplicationAttributesHandlers.put(HTTP_DELETE,
				new HandlerMethod(controller, "deleteApplicationAttributes",
						String.class));

		// service scope, single attribute
		final String singleServiceAttributeUri = "/attributes/services/"
				+ APPLICATION_NAME + "/" + SERVICE_NAME + "/" + ATTRIBUTE_NAME;
		final HashMap<String, HandlerMethod> singleServiceAttributeHandlers = new HashMap<String, HandlerMethod>();
		singleServiceAttributeHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getServiceAttribute", String.class, String.class,
				String.class));
		singleServiceAttributeHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setServiceAttribute", String.class, String.class,
				String.class, Object.class));
		singleServiceAttributeHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteServiceAttribute", String.class,
				String.class, String.class));

		// service scope, multiple attributes
		final String multipleServiceAttributesUri = "/attributes/services/"
				+ APPLICATION_NAME + "/" + SERVICE_NAME;
		final HashMap<String, HandlerMethod> multipleServiceAttributesHandlers = new HashMap<String, HandlerMethod>();
		multipleServiceAttributesHandlers
				.put(HTTP_GET, new HandlerMethod(controller,
						"getServiceAttributes", String.class, String.class));
		multipleServiceAttributesHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setServiceAttributes", String.class, String.class,
				Map.class));
		multipleServiceAttributesHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteServiceAttributes", String.class,
				String.class));

		// instance scope, single attribute
		final String singleInstanceAttributeUri = "/attributes/instances/"
				+ APPLICATION_NAME + "/" + SERVICE_NAME + "/" + INSTANCE_ID
				+ "/" + ATTRIBUTE_NAME;
		final HashMap<String, HandlerMethod> singleInstanceAttributeHandlers = new HashMap<String, HandlerMethod>();
		singleInstanceAttributeHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getInstanceAttribute", String.class, String.class,
				int.class, String.class));
		singleInstanceAttributeHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setInstanceAttribute", String.class, String.class,
				int.class, String.class, Object.class));
		singleInstanceAttributeHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteInstanceAttribute", String.class,
				String.class, int.class, String.class));

		// instance scope, multiple attributes
		final String multipleInstanceAttributesUri = "/attributes/instances/"
				+ APPLICATION_NAME + "/" + SERVICE_NAME + "/" + INSTANCE_ID;
		final HashMap<String, HandlerMethod> multipleInstanceAttributesHandlers = new HashMap<String, HandlerMethod>();
		multipleInstanceAttributesHandlers.put(HTTP_GET, new HandlerMethod(
				controller, "getInstanceAttributes", String.class,
				String.class, int.class));
		multipleInstanceAttributesHandlers.put(HTTP_POST, new HandlerMethod(
				controller, "setInstanceAttributes", String.class,
				String.class, int.class, Map.class));
		multipleInstanceAttributesHandlers.put(HTTP_DELETE, new HandlerMethod(
				controller, "deleteInstanceAttributes", String.class,
				String.class, int.class));

		singleAttributeUrisMapping.put(singleGlobalAttributeUri,
				singleGlobalAttributeHandlers);
		singleAttributeUrisMapping.put(singleApplicationAttributeUri,
				singleApplicationAttributeHandlers);
		singleAttributeUrisMapping.put(singleServiceAttributeUri,
				singleServiceAttributeHandlers);
		singleAttributeUrisMapping.put(singleInstanceAttributeUri,
				singleInstanceAttributeHandlers);

		multipleAttributesUrisMapping.put(multipleGlobalAttributesUri,
				multipleGlobalAttributesHandlers);
		multipleAttributesUrisMapping.put(multipleApplicationAttributesUri,
				multipleApplicationAttributesHandlers);
		multipleAttributesUrisMapping.put(multipleServiceAttributesUri,
				multipleServiceAttributesHandlers);
		multipleAttributesUrisMapping.put(multipleInstanceAttributesUri,
				multipleInstanceAttributesHandlers);

		// TODO: fix license
	}

	/**
	 * Test GET & POST calls for getting or setting a single attribute, in all 4
	 * scopes (global, application, service & instance).
	 * 
	 * @throws Exception
	 *             Indicates the GET / POST call failed.
	 */

	@Test
	public void testSingleAttribute() throws Exception {

		final Map<String, String> myInitialAttrMap = new HashMap<String, String>();
		myInitialAttrMap.put("myInitialAttrMapKey", "myInitialAttrMapValue");

		final Map<String, String> myUpdatedAttrMap = new HashMap<String, String>();
		myUpdatedAttrMap.put("myUpdatedAttrMapKey", "myUpdatedAttrMapValue");

		for (final String requestUri : singleAttributeUrisMapping.keySet()) {
			// test a string value
			testUriForSingleAttribute(requestUri, "myInitialAttrStringValue",
					"myUpdatedAttrStringValue");
			// test a map value
			testUriForSingleAttribute(requestUri, myInitialAttrMap,
					myUpdatedAttrMap);
		}
	}

	/**
	 * Test GET & POST calls for getting or setting multiple attributes at once,
	 * in all 4 scopes (global, application, service & instance).
	 * 
	 * @throws Exception
	 *             Indicates the GET / POST call failed.
	 */
	@Test
	public void testMultipleAttributes() throws Exception {

		final Map<String, String> myInitialAttrMap = new HashMap<String, String>();
		myInitialAttrMap.put("attrMapKey1", "myInitialAttrMapValue1");
		myInitialAttrMap.put("attrMapKey2", "myInitialAttrMapValue2");
		myInitialAttrMap.put("attrMapKey3", "myInitialAttrMapValue3");

		final Map<String, String> myUpdatedAttrMap = new HashMap<String, String>();
		myUpdatedAttrMap.put("attrMapKey1", "myUpdatedAttrMapValue1");
		myUpdatedAttrMap.put("attrMapKey2", "myUpdatedAttrMapValue2");
		myUpdatedAttrMap.put("attrMapKey3", "myUpdatedAttrMapValue3");

		for (final String requestUri : multipleAttributesUrisMapping.keySet()) {
			testUriForMultipleAttributes(requestUri, myInitialAttrMap,
					myUpdatedAttrMap);
		}
	}

	private void testUriForSingleAttribute(final String requestUri,
			final Object attrInitialValue, final Object attrUpdatedValue)
			throws Exception {

		final HashMap<String, HandlerMethod> uriHandlers = singleAttributeUrisMapping
				.get(requestUri);

		MockHttpServletRequest reqeust;
		final Map<String, Object> expectedMap = new HashMap<String, Object>();
		expectedMap.put(ATTRIBUTE_NAME, null);

		System.out.println("testing uri: " + requestUri);

		// Attempt to get the attribute before setting it.
		// Expecting null attribute value in return (the attribute does not
		// exists yet).
		reqeust = createMockGetRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_GET),
				convertToJson(expectedMap));

		// Set the attribute.
		// Expecting null attribute value in response content (post returns the
		// previous attribute value).
		reqeust = createMockPostRequest(requestUri,
				convertToJson(attrInitialValue));
		testRequest(reqeust, uriHandlers.get(HTTP_POST),
				convertToJson(expectedMap));

		// Set the attribute again.
		// Expecting the @attributeValue parameter as the response attribute
		// value.
		reqeust = createMockPostRequest(requestUri,
				convertToJson(attrUpdatedValue));
		expectedMap.put(ATTRIBUTE_NAME, attrInitialValue);
		testRequest(reqeust, uriHandlers.get(HTTP_POST),
				convertToJson(expectedMap));

		// Get the attribute.
		// Expecting NEW_VALUE_PREFIX + @attributeValue as the response
		// attribute value.
		reqeust = createMockGetRequest(requestUri);
		expectedMap.put(ATTRIBUTE_NAME, attrUpdatedValue);
		testRequest(reqeust, uriHandlers.get(HTTP_GET),
				convertToJson(expectedMap));

		// delete, to leave a "clean" space
		reqeust = createMockDeleteRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_DELETE),
				convertToJson(expectedMap));

		// Attempt to get the attribute after deleting it.
		// Expecting null attribute value in return (the attribute does not
		// exists).
		reqeust = createMockGetRequest(requestUri);
		expectedMap.put(ATTRIBUTE_NAME, null);
		testRequest(reqeust, uriHandlers.get(HTTP_GET),
				convertToJson(expectedMap));

		System.out.println("finished test for uri: " + requestUri);
	}

	private void testUriForMultipleAttributes(final String requestUri,
			final Map<String, String> attrInitialMap,
			final Map<String, String> attrUpdatedMap) throws Exception {

		final HashMap<String, HandlerMethod> uriHandlers = multipleAttributesUrisMapping
				.get(requestUri);
		MockHttpServletRequest reqeust;

		System.out.println("testing uri: " + requestUri);

		// Attempt to get the attribute before setting it.
		// Expecting null attribute value in return (the attribute does not
		// exists yet).
		reqeust = createMockGetRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_GET), "{}");

		// Set the attribute.
		// Expecting null attribute value in response content (post returns the
		// previous attribute value).
		reqeust = createMockPostRequest(requestUri,
				convertToJson(attrInitialMap));
		testRequest(reqeust, uriHandlers.get(HTTP_POST),
				"{\"status\":\"success\"}");

		// Set the attribute again.
		// Expecting the @attributeValue parameter as the response attribute
		// value.
		reqeust = createMockPostRequest(requestUri,
				convertToJson(attrUpdatedMap));
		testRequest(reqeust, uriHandlers.get(HTTP_POST),
				"{\"status\":\"success\"}");

		/*
		 * Map<String, Object> merged = new HashMap<String, Object>();
		 * merged.putAll(attrInitialMap); merged.putAll(attrUpdatedMap);
		 */

		// Get the attribute.
		// Expecting NEW_VALUE_PREFIX + @attributeValue as the response
		// attribute value.
		reqeust = createMockGetRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_GET),
				convertToJson(attrUpdatedMap));

		// delete, to leave a "clean" space
		reqeust = createMockDeleteRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_DELETE),
				convertToJson(attrUpdatedMap));

		// Attempt to get the attribute after deleting it.
		// Expecting null attribute value in return (the attribute does not
		// exists).
		reqeust = createMockGetRequest(requestUri);
		testRequest(reqeust, uriHandlers.get(HTTP_GET),
				convertToJson(new HashMap<String, Object>()));

		System.out.println("finished test for uri: " + requestUri);
	}

	private void testRequest(final MockHttpServletRequest reqeust,
			final HandlerMethod expectedHandlerMethod,
			final String expectedResponseContent) throws Exception {

		final MockHttpServletResponse response = new MockHttpServletResponse();

		final Object handler = getHandlerToRequest(reqeust);
		Assert.assertEquals("Wrong handler selected for request uri: "
				+ reqeust.getRequestURI(), expectedHandlerMethod.toString(),
				handler.toString());

		// handle the request
		handlerAdapter.handle(reqeust, response, handler);

		// validate the response
		Assert.assertTrue("Wrong response status: " + response.getStatus(),
				response.getStatus() == HttpStatus.OK.value());
		Assert.assertEquals(
				"Wrong content type in response: " + response.getContentType(),
				JSON_CONTENT_TYPE, response.getContentType());
		Assert.assertEquals(
				"Wrong response content: " + response.getContentAsString(),
				expectedResponseContent, response.getContentAsString());
	}

	/**
	 * This method finds the handler for a given request URI. It will also
	 * ensure that the URI Parameters i.e. /context/test/{name} are added to the
	 * request
	 * 
	 * @param request
	 *            The request object to be used
	 * @return The correct handler for the request
	 * @throws Exception
	 *             Indicates a matching handler could not be found
	 */
	private Object getHandlerToRequest(final MockHttpServletRequest request)
			throws Exception {
		HandlerExecutionChain chain = null;

		final Map<String, HandlerMapping> map = applicationContext
				.getBeansOfType(HandlerMapping.class);

        for (HandlerMapping mapping : map.values()) {
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

		return chain.getHandler();
	}

	private MockHttpServletRequest createMockGetRequest(final String requestUri) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(requestUri);
		request.setMethod("GET");
		request.setContentType("application/json");

		return request;
	}

	private MockHttpServletRequest createMockPostRequest(
			final String requestUri, final String contentAsJson) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(requestUri);
		request.setMethod("POST");
		request.setContentType("application/json");

		if (StringUtils.isNotBlank(contentAsJson)) {
			request.setContent(contentAsJson.getBytes());
		}

		return request;
	}

	private MockHttpServletRequest createMockDeleteRequest(
			final String requestUri) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(requestUri);
		request.setMethod("DELETE");
		request.setContentType("application/json");

		return request;
	}

	/**
	 * Converts a json String to a Map<String, Object>.
	 * 
	 * @param jsonStr
	 *            a json-format String to convert to a map
	 * @return a Map<String, Object> based on the given json-format String
	 * @throws IOException
	 *             Reporting failure to read or convert the json-format string
	 *             to a map
	 */
	public static Map<String, Object> jsonToMap(final String jsonStr)
			throws IOException {
		return PROJECT_MAPPER.readValue(jsonStr, TypeFactory.type(Map.class));
	}

	/**
	 * Converts a Map<String, ?> to a json-format String.
	 * 
	 * @param map
	 *            a map to convert to json-format String
	 * @return a json-format String based on the given map
	 * @throws IOException
	 *             Reporting failure to read the map or convert it
	 */
	// private static String mapToJson(final Map<String, ?> map) throws
	// IOException {
	// return PROJECT_MAPPER.writeValueAsString(map);
	// }

	/**
	 * Converts a json-format String to standard java String.
	 * 
	 * @param strValue
	 *            a string value to convert to json-format String
	 * @return a json-format String based on the given value
	 * @throws IOException
	 *             Reporting failure to read the string value or convert it
	 */
	// private static String jsonToString(final String jsonStr) throws
	// IOException {
	// return PROJECT_MAPPER.readValue(jsonStr, TypeFactory.type(String.class));
	// }

	/**
	 * Converts a String to a json-format String.
	 * 
	 * @param strValue
	 *            a string value to convert to json-format String
	 * @return a json-format String based on the given value
	 * @throws IOException
	 *             Reporting failure to read the string value or convert it
	 */
	// private static String stringToJson(final String strValue) throws
	// IOException {
	// return PROJECT_MAPPER.writeValueAsString(strValue);
	// }

	/**
	 * Converts an object to a json-format String.
	 * 
	 * @param value
	 *            an object to convert to json-format String
	 * @return a json-format String based on the given object
	 * @throws IOException
	 *             Reporting failure to convert the object
	 */
	private static String convertToJson(final Object value) throws IOException {
		return PROJECT_MAPPER.writeValueAsString(value);
	}
}