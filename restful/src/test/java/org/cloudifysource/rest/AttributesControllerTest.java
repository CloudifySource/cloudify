/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudifysource.rest.controllers.AttributesController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

/**
 * This class tests different calls (get/post) to the attributes controller web service, over REST. 
 * The new spring REST testing framework is being used.
 *
 * @author noak
 */
// Swap the default JUnit4 with the spring specific SpringJUnit4ClassRunner.
// This will allow spring to inject the application context
@RunWith(SpringJUnit4ClassRunner.class)
// Setup the configuration of the application context and the web mvc layer
@ContextConfiguration({ "classpath:META-INF/spring/applicationContext.xml",
        "classpath:META-INF/spring/webmvc-config-test.xml" })
public class AttributesControllerTest extends ControllerTest {
    private static final String APPLICATION_NAME = "petclinic-simple";
    private static final String SERVICE_NAME = "tomcat";
    private static final String INSTANCE_ID = "1";
    private static final String ATTRIBUTE_NAME = "myAttr";

    private List<String> singleAttributeUris;
    private List<String> multipleAttributesUris;
    private HashMap<String, HashMap<RequestMethod, HandlerMethod>> controllerMapping;


    public HandlerMethod getExpectedMethod(final String requestUri, final RequestMethod requestMethod) {
        final HashMap<RequestMethod, HandlerMethod> uriMap = controllerMapping.get(requestUri);
        Assert.assertNotNull(uriMap);
        return uriMap.get(requestMethod);
    }

    /**
     * Initialize the basic objects that are used widely in the tests.
     * @return the mapping from uri and http method to the correct handler method
     * @throws NoSuchMethodException
     *             Indicates the defined {@link HandlerMethod} does not exist
     */
    @Before
    public void init() throws NoSuchMethodException {
        AttributesController controller = applicationContext.getBean(AttributesController.class);

        singleAttributeUris = new LinkedList<String>();
        multipleAttributesUris = new LinkedList<String>();

        // global scope, single attribute
        final String singleGlobalAttributeUri = "/attributes/globals/" + ATTRIBUTE_NAME;
        singleAttributeUris.add(singleGlobalAttributeUri);
        final HashMap<RequestMethod, HandlerMethod> singleGlobalAttributeHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        singleGlobalAttributeHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getGlobalAttribute", String.class));
        singleGlobalAttributeHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setGlobalAttribute", String.class, Object.class));
        singleGlobalAttributeHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteGlobalAttribute", String.class));

        // global scope, multiple attributes
        final String multipleGlobalAttributesUri = "/attributes/globals";
        multipleAttributesUris.add(multipleGlobalAttributesUri);
        final HashMap<RequestMethod, HandlerMethod> multipleGlobalAttributesHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        multipleGlobalAttributesHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getGlobalAttributes"));
        multipleGlobalAttributesHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setGlobalAttributes", Map.class));
        multipleGlobalAttributesHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteGlobalAttributes"));

        // application scope, single attribute
        final String singleApplicationAttributeUri = "/attributes/applications/"
                + APPLICATION_NAME + "/" + ATTRIBUTE_NAME;
        singleAttributeUris.add(singleApplicationAttributeUri);
        final HashMap<RequestMethod, HandlerMethod> singleApplicationAttributeHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        singleApplicationAttributeHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getApplicationAttribute", String.class,
                String.class));
        singleApplicationAttributeHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setApplicationAttribute", String.class,
                String.class, Object.class));
        singleApplicationAttributeHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteApplicationAttribute", String.class,
                String.class));

        // application scope, multiple attributes
        final String multipleApplicationAttributesUri = "/attributes/applications/" + APPLICATION_NAME;
        multipleAttributesUris.add(multipleApplicationAttributesUri);
        final HashMap<RequestMethod, HandlerMethod> multipleApplicationAttributesHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        multipleApplicationAttributesHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getApplicationAttributes", String.class));
        multipleApplicationAttributesHandlers.put(RequestMethod.POST,
                new HandlerMethod(controller, "setApplicationAttributes",
                        String.class, Map.class));
        multipleApplicationAttributesHandlers.put(RequestMethod.DELETE,
                new HandlerMethod(controller, "deleteApplicationAttributes",
                        String.class));

        // service scope, single attribute
        final String singleServiceAttributeUri = "/attributes/services/"
                + APPLICATION_NAME + "/" + SERVICE_NAME + "/" + ATTRIBUTE_NAME;
        singleAttributeUris.add(singleServiceAttributeUri);
        final HashMap<RequestMethod, HandlerMethod> singleServiceAttributeHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        singleServiceAttributeHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getServiceAttribute", String.class, String.class,
                String.class));
        singleServiceAttributeHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setServiceAttribute", String.class, String.class,
                String.class, Object.class));
        singleServiceAttributeHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteServiceAttribute", String.class,
                String.class, String.class));

        // service scope, multiple attributes
        final String multipleServiceAttributesUri = "/attributes/services/"
                + APPLICATION_NAME + "/" + SERVICE_NAME;
        multipleAttributesUris.add(multipleServiceAttributesUri);
        final HashMap<RequestMethod, HandlerMethod> multipleServiceAttributesHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        multipleServiceAttributesHandlers
                .put(RequestMethod.GET, new HandlerMethod(controller,
                        "getServiceAttributes", String.class, String.class));
        multipleServiceAttributesHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setServiceAttributes", String.class, String.class, Map.class));
        multipleServiceAttributesHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteServiceAttributes", String.class, String.class));

        // instance scope, single attribute
        final String singleInstanceAttributeUri = "/attributes/instances/"
                + APPLICATION_NAME + "/" + SERVICE_NAME + "/" + INSTANCE_ID
                + "/" + ATTRIBUTE_NAME;
        singleAttributeUris.add(singleInstanceAttributeUri);
        final HashMap<RequestMethod, HandlerMethod> singleInstanceAttributeHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        singleInstanceAttributeHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getInstanceAttribute", String.class, String.class,
                int.class, String.class));
        singleInstanceAttributeHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setInstanceAttribute", String.class, String.class,
                int.class, String.class, Object.class));
        singleInstanceAttributeHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteInstanceAttribute", String.class,
                String.class, int.class, String.class));

        // instance scope, multiple attributes
        final String multipleInstanceAttributesUri = "/attributes/instances/"
                + APPLICATION_NAME + "/" + SERVICE_NAME + "/" + INSTANCE_ID;
        multipleAttributesUris.add(multipleInstanceAttributesUri);
        final HashMap<RequestMethod, HandlerMethod> multipleInstanceAttributesHandlers =
                new HashMap<RequestMethod, HandlerMethod>();
        multipleInstanceAttributesHandlers.put(RequestMethod.GET, new HandlerMethod(
                controller, "getInstanceAttributes", String.class,
                String.class, int.class));
        multipleInstanceAttributesHandlers.put(RequestMethod.POST, new HandlerMethod(
                controller, "setInstanceAttributes", String.class,
                String.class, int.class, Map.class));
        multipleInstanceAttributesHandlers.put(RequestMethod.DELETE, new HandlerMethod(
                controller, "deleteInstanceAttributes", String.class,
                String.class, int.class));

        controllerMapping = new HashMap<String, HashMap<RequestMethod, HandlerMethod>>();
        controllerMapping.put(singleGlobalAttributeUri, singleGlobalAttributeHandlers);
        controllerMapping.put(singleApplicationAttributeUri, singleApplicationAttributeHandlers);
        controllerMapping.put(singleServiceAttributeUri, singleServiceAttributeHandlers);
        controllerMapping.put(singleInstanceAttributeUri, singleInstanceAttributeHandlers);
        controllerMapping.put(multipleGlobalAttributesUri, multipleGlobalAttributesHandlers);
        controllerMapping.put(multipleApplicationAttributesUri, multipleApplicationAttributesHandlers);
        controllerMapping.put(multipleServiceAttributesUri, multipleServiceAttributesHandlers);
        controllerMapping.put(multipleInstanceAttributesUri, multipleInstanceAttributesHandlers);

        // TODO: fix license
    }

    /**
     * Test GET & POST calls for getting or setting a single attribute, in all 4 scopes (global, application, service &
     * instance).
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

        for (final String requestUri : singleAttributeUris) {
            // test a string value
            testUriForSingleAttribute(requestUri, "myInitialAttrStringValue",
                    "myUpdatedAttrStringValue");
            // test a map value
            testUriForSingleAttribute(requestUri, myInitialAttrMap,
                    myUpdatedAttrMap);
        }
    }

    /**
     * Test GET & POST calls for getting or setting multiple attributes at once, in all 4 scopes (global, application,
     * service & instance).
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

        for (final String requestUri : multipleAttributesUris) {
            testUriForMultipleAttributes(requestUri, myInitialAttrMap, myUpdatedAttrMap);
        }
    }

    private void testUriForSingleAttribute(final String requestUri,
                                           final Object attrInitialValue, final Object attrUpdatedValue)
            throws Exception {

        final Map<String, Object> expectedMap = new HashMap<String, Object>();
        expectedMap.put(ATTRIBUTE_NAME, null);

        System.out.println("testing uri: " + requestUri);

        // Attempt to get the attribute before setting it.
        // Expecting null attribute value in return (the attribute does not
        // exists yet).
        testGet(requestUri, convertToJson(expectedMap));

        // Set the attribute.
        // Expecting null attribute value in response content (post returns the
        // previous attribute value).
        testPost(requestUri, convertToJson(attrInitialValue), convertToJson(expectedMap));

        // Set the attribute again.
        // Expecting the @attributeValue parameter as the response attribute
        // value.
        expectedMap.put(ATTRIBUTE_NAME, attrInitialValue);
        testPost(requestUri, convertToJson(attrUpdatedValue), convertToJson(expectedMap));

        // Get the attribute.
        // Expecting NEW_VALUE_PREFIX + @attributeValue as the response
        // attribute value.
        expectedMap.put(ATTRIBUTE_NAME, attrUpdatedValue);
        testGet(requestUri, convertToJson(expectedMap));

        // delete, to leave a "clean" space
        testDelete(requestUri, convertToJson(expectedMap));

        // Attempt to get the attribute after deleting it.
        // Expecting null attribute value in return (the attribute does not
        // exists).
        expectedMap.put(ATTRIBUTE_NAME, null);
        testGet(requestUri, convertToJson(expectedMap));

        System.out.println("finished test for uri: " + requestUri);
    }

    private void testUriForMultipleAttributes(final String requestUri,
                                              final Map<String, String> attrInitialMap,
                                              final Map<String, String> attrUpdatedMap) throws Exception {

        System.out.println("testing uri: " + requestUri);

        // Attempt to get the attribute before setting it.
        // Expecting null attribute value in return (the attribute does not
        // exists yet).
        testGet(requestUri, "{}");

        // Set the attribute.
        // Expecting null attribute value in response content (post returns the
        // previous attribute value).
        testPost(requestUri, convertToJson(attrInitialMap), "{\"status\":\"success\"}");

        // Set the attribute again.
        // Expecting the @attributeValue parameter as the response attribute
        // value.
        testPost(requestUri, convertToJson(attrUpdatedMap), "{\"status\":\"success\"}");

		/*
		 * Map<String, Object> merged = new HashMap<String, Object>(); merged.putAll(attrInitialMap);
		 * merged.putAll(attrUpdatedMap);
		 */

        // Get the attribute.
        // Expecting NEW_VALUE_PREFIX + @attributeValue as the response
        // attribute value.
        testGet(requestUri, convertToJson(attrUpdatedMap));

        // delete, to leave a "clean" space
        testDelete(requestUri, convertToJson(attrUpdatedMap));

        // Attempt to get the attribute after deleting it.
        // Expecting null attribute value in return (the attribute does not
        // exists).
        testGet(requestUri, convertToJson(new HashMap<String, Object>()));

        System.out.println("finished test for uri: " + requestUri);
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
}