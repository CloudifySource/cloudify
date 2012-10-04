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
package org.cloudifysource.rest.controllers;

import com.gigaspaces.client.WriteModifiers;
import net.jini.core.lease.Lease;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.*;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restDoclet.annotations.JsonRequestExample;
import org.cloudifysource.restDoclet.annotations.JsonResponseExample;
import org.cloudifysource.restDoclet.annotations.PossibleResponseStatuses;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.cloudifysource.rest.util.RestUtils.successStatus;

/**
 * @author noak
 * @since 2.2.0
 */
@Controller
@RequestMapping("/attributes")
public class AttributesController {

    @GigaSpaceContext(name = "gigaSpace")
    private GigaSpace gigaSpace;

    private static final Logger logger = Logger.getLogger(AttributesController.class.getName());

    /**
     * Gets an attribute value, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @param attributeName   The name (key) of the attribute to get.
     * @return The attribute's value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}/{attributeName}",
            method = RequestMethod.GET)
    @ResponseBody
    public Object getInstanceAttribute(@PathVariable final String applicationName,
                                       @PathVariable final String serviceName, @PathVariable final int instanceId,
                                       @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get attribute " + attributeName + " of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName) + " instance number " + instanceId
                    + " of application " + applicationName);
        }

        InstanceCloudifyAttribute templateAttribute = new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, attributeName, null);
        //read the matching attribute from the space
        InstanceCloudifyAttribute valueEntry = gigaSpace.read(templateAttribute);
        Object value = (valueEntry != null) ? valueEntry.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Gets multiple attributes' values, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @return a Map containing the attributes' names (keys) and values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}",
            method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getInstanceAttributes(@PathVariable final String applicationName,
                                                     @PathVariable final String serviceName, @PathVariable final int instanceId) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get all attributes of instance number " + instanceId + " of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName) + " of application "
                    + applicationName);
        }

        InstanceCloudifyAttribute templateAttribute = new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, null, null);
        // read the matching multiple attributes from the space
        InstanceCloudifyAttribute[] attributes = gigaSpace.readMultiple(templateAttribute);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        for (final InstanceCloudifyAttribute attribute : attributes) {
            attributesMap.put(attribute.getKey(), attribute.getValue());
        }

        return attributesMap;
    }

    /**
     * Gets an attribute value, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param attributeName   The name (key) of the attribute to get.
     * @return The attribute's value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}/{attributeName}",
            method = RequestMethod.GET)
    @ResponseBody
    public Object getServiceAttribute(@PathVariable final String applicationName,
                                      @PathVariable final String serviceName, 
                                      @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get attribute " + attributeName + " of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        ServiceCloudifyAttribute templateAttribute = new ServiceCloudifyAttribute(applicationName, serviceName, attributeName, null);
        //read the matching attribute from the space
        ServiceCloudifyAttribute valueEntry = gigaSpace.read(templateAttribute);
        Object value = (valueEntry != null) ? valueEntry.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Gets multiple attributes' values, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @return a Map containing the attributes' names (keys) and values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getServiceAttributes(@PathVariable final String applicationName,
                                                    @PathVariable final String serviceName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get all attributes of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        ServiceCloudifyAttribute templateAttribute = new ServiceCloudifyAttribute(applicationName, serviceName, null, null);
        // read the matching multiple attributes from the space
        ServiceCloudifyAttribute[] attributes = gigaSpace.readMultiple(templateAttribute);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        for (final ServiceCloudifyAttribute attribute : attributes) {
            attributesMap.put(attribute.getKey(), attribute.getValue());
        }

        return attributesMap;
    }

    /**
     * Gets an attribute value, scope: application attributes.
     *
     * @param applicationName The application name.
     * @param attributeName   The name (key) of the attribute to get.
     * @return The attribute's value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}/{attributeName}", method = RequestMethod.GET)
    @ResponseBody
    public Object getApplicationAttribute(@PathVariable final String applicationName,
                                          @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get attribute " + attributeName + " of application " + applicationName);
        }

        ApplicationCloudifyAttribute templateAttribute = new ApplicationCloudifyAttribute(applicationName, attributeName, null);
        //read the matching attribute from the space
        ApplicationCloudifyAttribute valueEntry = gigaSpace.read(templateAttribute);
        Object value = (valueEntry != null) ? valueEntry.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Gets multiple attributes' values, scope: application attributes.
     *
     * @param applicationName The application name.
     * @return a Map containing the attributes' names (keys) and values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getApplicationAttributes(@PathVariable final String applicationName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get all attributes of application " + applicationName);
        }

        ApplicationCloudifyAttribute templateAttribute = new ApplicationCloudifyAttribute(applicationName, null, null);
        // read the matching multiple attributes from the space
        ApplicationCloudifyAttribute[] attributes = gigaSpace.readMultiple(templateAttribute);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        for (final ApplicationCloudifyAttribute attribute : attributes) {
            attributesMap.put(attribute.getKey(), attribute.getValue());
        }

        return attributesMap;
    }

    /**
     * Gets an attribute value, scope: global attributes.
     *
     * @param attributeName The name (key) of the attribute to get.
     * @return The attribute's value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals/{attributeName}", method = RequestMethod.GET)
    @ResponseBody
    public Object getGlobalAttribute(@PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get global attribute " + attributeName);
        }

        GlobalCloudifyAttribute templateAttribute = new GlobalCloudifyAttribute(attributeName, null);
        //read the matching attribute from the space
        GlobalCloudifyAttribute valueEntry = gigaSpace.read(templateAttribute);
        Object value = (valueEntry != null) ? valueEntry.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Gets multiple attributes' values, scope: global attributes.
     *
     * @return a Map containing the attributes' names (keys) and values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getGlobalAttributes() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to get all global attributes");
        }

        GlobalCloudifyAttribute templateAttribute = new GlobalCloudifyAttribute();
        // read the matching multiple attributes from the space
        GlobalCloudifyAttribute[] attributes = gigaSpace.readMultiple(templateAttribute);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        for (final GlobalCloudifyAttribute attribute : attributes) {
            attributesMap.put(attribute.getKey(), attribute.getValue());
        }

        return attributesMap;
    }

    /**
     * Sets an attribute value, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @param attributeName   The name of the attribute to get.
     * @param attributeValue  The value to set.
     * @return The previous value.
     */
    @JsonRequestExample(requestBody = "\"attributeValue\"")
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before changed)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}/{attributeName}",
            method = RequestMethod.POST)
    @ResponseBody
    public Object setInstanceAttribute(@PathVariable final String applicationName,
                                       @PathVariable final String serviceName, @PathVariable final int instanceId,
                                       @PathVariable final String attributeName, @RequestBody final Object attributeValue) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set attribute " + attributeName + " of instance number " + instanceId
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName + " to: " + attributeValue);
        }

        InstanceCloudifyAttribute attribute = new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, attributeName, null);
        // take (delete)
        InstanceCloudifyAttribute previousValue = gigaSpace.take(attribute);
        // write
        attribute.setValue(attributeValue);
        gigaSpace.write(attribute);
        Object value = (previousValue != null) ? previousValue.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Sets multiple attributes' values, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @param attributesMap   a Map containing the attributes' names (keys) and values to set.
     * @return The status of the operation.
     */
    @JsonRequestExample(requestBody = "{\"attribute1Name\":\"attribute1Value\", \"attribute2Name\":\"attribute2Value\"}")
    @JsonResponseExample(status = "success")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}",
            method = RequestMethod.POST)
    @ResponseBody
    public Object setInstanceAttributes(@PathVariable final String applicationName,
                                        @PathVariable final String serviceName, @PathVariable final int instanceId,
                                        @RequestBody final Map<String, Object> attributesMap) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set multiple attributes of instance number " + instanceId
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }
        writeAttributesToSpace(applicationName, serviceName, instanceId, attributesMap);
        return successStatus();
    }

    /**
     * Sets an attribute value, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param attributeName   The name of the attribute to get.
     * @param attributeValue  The value to set.
     * @return The previous value.
     */
    @JsonRequestExample(requestBody = "\"attributeValue\"")
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before changed)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}/{attributeName}",
            method = RequestMethod.POST)
    @ResponseBody
    public Object setServiceAttribute(@PathVariable final String applicationName,
                                      @PathVariable final String serviceName, @PathVariable final String attributeName,
                                      @RequestBody final Object attributeValue) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set attribute " + attributeName
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName + " to: " + attributeValue);
        }

        ServiceCloudifyAttribute attribute = new ServiceCloudifyAttribute(applicationName, serviceName, attributeName, null);
        // take (delete)
        ServiceCloudifyAttribute previousValue = gigaSpace.take(attribute);
        // write
        attribute.setValue(attributeValue);
        gigaSpace.write(attribute);
        Object value = (previousValue != null) ? previousValue.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Sets multiple attributes' values, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param attributesMap   a Map containing the attributes' names (keys) and values to set.
     * @return A map of the previous values.
     */
    @JsonRequestExample(requestBody = "{\"attribute1Name\":\"attribute1Value\", \"attribute2Name\":\"attribute2Value\"}")
    @JsonResponseExample(status = "success")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}", method = RequestMethod.POST)
    @ResponseBody
    public Object setServiceAttributes(@PathVariable final String applicationName,
                                       @PathVariable final String serviceName, @RequestBody final Map<String, Object> attributesMap) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set multiple attributes of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        writeAttributesToSpace(applicationName, serviceName, null, attributesMap);
        return successStatus();
    }

    /**
     * Sets an attribute value, scope: application attributes.
     *
     * @param applicationName The application name.
     * @param attributeName   The name of the attribute to get.
     * @param attributeValue  The value to set.
     * @return The previous value.
     */
    @JsonRequestExample(requestBody = "\"attributeValue\"")
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before changed)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}/{attributeName}", method = RequestMethod.POST)
    @ResponseBody
    public Object setApplicationAttribute(@PathVariable final String applicationName,
                                          @PathVariable final String attributeName, @RequestBody final Object attributeValue) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set attribute " + attributeName
                    + " of application " + applicationName + " to: " + attributeValue);
        }

        ApplicationCloudifyAttribute attribute = new ApplicationCloudifyAttribute(applicationName, attributeName, null);
        // take (delete)
        ApplicationCloudifyAttribute previousValue = gigaSpace.take(attribute);
        // write
        attribute.setValue(attributeValue);
        gigaSpace.write(attribute);
        Object value = (previousValue != null) ? previousValue.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Sets multiple attributes' values, scope: application attributes.
     *
     * @param applicationName The application name.
     * @param attributesMap   a Map containing the attributes' names (keys) and values to set.
     * @return A map of the previous values.
     */
    @JsonRequestExample(requestBody = "{\"attribute1Name\":\"attribute1Value\", \"attribute2Name\":\"attribute2Value\"}")
    @JsonResponseExample(status = "success")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}", method = RequestMethod.POST)
    @ResponseBody
    public Object setApplicationAttributes(@PathVariable final String applicationName,
                                           @RequestBody final Map<String, Object> attributesMap) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set multiple attributes of application " + applicationName);
        }

        // write to space
        writeAttributesToSpace(applicationName, null, null, attributesMap);
        return successStatus();
    }

    private void writeAttributesToSpace(String applicationName, String serviceName, Integer instanceId, Map<String, Object> attributesMap) {
        AbstractCloudifyAttribute[] attributesToWrite = new AbstractCloudifyAttribute[attributesMap.size()];
        int i = 0;
        for (final Entry<String, Object> attrEntry : attributesMap.entrySet()) {
            AbstractCloudifyAttribute newAttr = createCloudifyAttribute(applicationName, serviceName, instanceId, attrEntry.getKey(), attrEntry.getValue());
            attributesToWrite[i++] = newAttr;
        }
        gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }


    private AbstractCloudifyAttribute createCloudifyAttribute(String applicationName, String serviceName, Integer instanceId, String name, Object value) {
        if (applicationName == null) {
            return new GlobalCloudifyAttribute(name, value);
        } else {
            if (serviceName == null) {
                return new ApplicationCloudifyAttribute(applicationName, name, value);
            } else {
                if (instanceId == null) {
                    return new ServiceCloudifyAttribute(applicationName, serviceName, name, value);
                } else {
                    return new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, name, value);
                }
            }
        }
    }

    /**
     * Sets an attribute value, scope: global attributes.
     *
     * @param attributeName  The name of the attribute to get.
     * @param attributeValue The value to set.
     * @return The previous value.
     */
    @JsonRequestExample(requestBody = "\"attributeValue\"")
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before changed)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals/{attributeName}", method = RequestMethod.POST)
    @ResponseBody
    public Object setGlobalAttribute(@PathVariable final String attributeName,
                                     @RequestBody final Object attributeValue) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set global attribute " + attributeName + " to: " + attributeValue);
        }

        GlobalCloudifyAttribute attribute = new GlobalCloudifyAttribute(attributeName, null);
        // take (delete)
        GlobalCloudifyAttribute previousValue = gigaSpace.take(attribute);
        // write
        attribute.setValue(attributeValue);
        gigaSpace.write(attribute);
        Object value = (previousValue != null) ? previousValue.getValue() : null;

        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Sets multiple attributes' values, scope: global attributes.
     *
     * @param attributesMap a Map containing the attributes' names (keys) and values to set.
     * @return A map of the previous values.
     */
    @JsonRequestExample(requestBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}")
    @JsonResponseExample(status = "success")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals", method = RequestMethod.POST)
    @ResponseBody
    public Object setGlobalAttributes(@RequestBody final Map<String, Object> attributesMap) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to set multiple global attributes");
        }
        // write multiple
        writeAttributesToSpace(null, null, null, attributesMap);
        return successStatus();
    }

    /**
     * Deletes an attribute value, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @param attributeName   The name of the attribute to get.
     * @return The previous value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}/{attributeName}",
            method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteInstanceAttribute(@PathVariable final String applicationName,
                                          @PathVariable final String serviceName, @PathVariable final int instanceId,
                                          @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete attribute " + attributeName + " of instance number " + instanceId
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        InstanceCloudifyAttribute attribute = new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, attributeName, null);
        // take (delete)
        InstanceCloudifyAttribute previousValue = gigaSpace.take(attribute);

        Object value = (previousValue != null) ? previousValue.getValue() : null;
        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Deletes multiple attributes, scope: instance attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param instanceId      The service instance id.
     * @return A map of the previous values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}", 
    		comments = "attribute1Value and attribute2Value are the previous values (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "instances/{applicationName}/{serviceName}/{instanceId}",
            method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteInstanceAttributes(@PathVariable final String applicationName,
                                           @PathVariable final String serviceName, @PathVariable final int instanceId) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete multiple attributes of instance number " + instanceId
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        InstanceCloudifyAttribute attribute = new InstanceCloudifyAttribute(applicationName, serviceName, instanceId, null, null);
        // take (delete) multiple
        InstanceCloudifyAttribute[] previousAttributesArr = gigaSpace.takeMultiple(attribute);
        final Map<String, Object> previousAttributesMap = new HashMap<String, Object>();
        for (final InstanceCloudifyAttribute previousAttr : previousAttributesArr) {
            previousAttributesMap.put(previousAttr.getKey(), previousAttr.getValue());
        }

        return previousAttributesMap;
    }

    /**
     * Deletes an attribute value, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @param attributeName   The name of the attribute to get.
     * @return The previous value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}/{attributeName}",
            method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteServiceAttribute(@PathVariable final String applicationName,
                                         @PathVariable final String serviceName, @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete attribute " + attributeName
                    + " of service " + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        ServiceCloudifyAttribute attribute = new ServiceCloudifyAttribute(applicationName, serviceName, attributeName, null);
        // take (delete)
        ServiceCloudifyAttribute previousValue = gigaSpace.take(attribute);

        Object value = (previousValue != null) ? previousValue.getValue() : null;
        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Deletes multiple attributes, scope: service attributes.
     *
     * @param applicationName The application name.
     * @param serviceName     The service name.
     * @return A map of the previous values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}", 
    		comments = "attribute1Value and attribute2Value are the previous values (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "services/{applicationName}/{serviceName}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteServiceAttributes(@PathVariable final String applicationName,
                                          @PathVariable final String serviceName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete multiple attributes of service "
                    + ServiceUtils.getAbsolutePUName(applicationName, serviceName)
                    + " of application " + applicationName);
        }

        ServiceCloudifyAttribute attribute = new ServiceCloudifyAttribute(applicationName, serviceName, null, null);
        // take (delete) multiple
        ServiceCloudifyAttribute[] previousAttributesArr = gigaSpace.takeMultiple(attribute);
        final Map<String, Object> previousAttributesMap = new HashMap<String, Object>();
        for (final ServiceCloudifyAttribute previousAttr : previousAttributesArr) {
            previousAttributesMap.put(previousAttr.getKey(), previousAttr.getValue());
        }

        return previousAttributesMap;
    }

    /**
     * Deletes an attribute value, scope: application attributes.
     *
     * @param applicationName The application name.
     * @param attributeName   The name of the attribute to get.
     * @return The previous value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}/{attributeName}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteApplicationAttribute(@PathVariable final String applicationName,
                                             @PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete attribute " + attributeName
                    + " of application " + applicationName);
        }

        ApplicationCloudifyAttribute attribute = new ApplicationCloudifyAttribute(applicationName, attributeName, null);
        // take (delete)
        ApplicationCloudifyAttribute previousValue = gigaSpace.take(attribute);

        Object value = (previousValue != null) ? previousValue.getValue() : null;
        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Deletes multiple attributes, scope: application attributes.
     *
     * @param applicationName The application name.
     * @return A map of the previous values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}", 
    		comments = "attribute1Value and attribute2Value are the previous values (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "applications/{applicationName}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteApplicationAttributes(@PathVariable final String applicationName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete multiple attributes of application " + applicationName);
        }

        ApplicationCloudifyAttribute attribute = new ApplicationCloudifyAttribute(applicationName, null, null);
        // take (delete) multiple
        ApplicationCloudifyAttribute[] previousAttributesArr = gigaSpace.takeMultiple(attribute);
        final Map<String, Object> previousAttributesMap = new HashMap<String, Object>();
        for (final ApplicationCloudifyAttribute previousAttr : previousAttributesArr) {
            previousAttributesMap.put(previousAttr.getKey(), previousAttr.getValue());
        }

        return previousAttributesMap;
    }

    /**
     * Deletes an attribute, scope: global attributes.
     *
     * @param attributeName The name of the attribute to delete.
     * @return The previous value.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attributeName\":\"attributeValue\"}", comments = "attributeValue is the previous value (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals/{attributeName}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteGlobalAttribute(@PathVariable final String attributeName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete global attribute: " + attributeName);
        }

        GlobalCloudifyAttribute attribute = new GlobalCloudifyAttribute(attributeName, null);
        // take (delete)
        GlobalCloudifyAttribute previousValue = gigaSpace.take(attribute);

        Object value = (previousValue != null) ? previousValue.getValue() : null;
        Map<String, Object> mapResult = new HashMap<String, Object>();
        mapResult.put(attributeName, value);
        return mapResult;
    }

    /**
     * Deletes multiple attributes, scope: global attributes.
     *
     * @return A list of the previous values.
     */
    @JsonResponseExample(status = "success", responseBody = "{\"attribute1Name\":\"attribute1Value\",\"attribute2Name\":\"attribute2Value\"}", 
    		comments = "attribute1Value and attribute2Value are the previous values (before deleted)")
    @PossibleResponseStatuses(codes = {200}, descriptions = {"success"})
    @RequestMapping(value = "globals", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteGlobalAttributes() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("received request to delete multiple global attributes");
        }

        GlobalCloudifyAttribute attribute = new GlobalCloudifyAttribute();
        // take (delete) multiple
        GlobalCloudifyAttribute[] previousAttributesArr = gigaSpace.takeMultiple(attribute);
        final Map<String, Object> previousAttributesMap = new HashMap<String, Object>();
        for (final GlobalCloudifyAttribute previousAttr : previousAttributesArr) {
            previousAttributesMap.put(previousAttr.getKey(), previousAttr.getValue());
        }
        return previousAttributesMap;
    }

}
