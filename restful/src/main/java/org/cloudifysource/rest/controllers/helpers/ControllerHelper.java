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
package org.cloudifysource.rest.controllers.helpers;

import java.util.HashMap;
import java.util.Map;

import net.jini.core.lease.Lease;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.AbstractCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ApplicationCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.GlobalCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.InstanceCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ServiceCloudifyAttribute;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;

import com.gigaspaces.client.WriteModifiers;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 1:11 PM
 */
public class ControllerHelper {

    private GigaSpace gigaSpace;
    private Admin admin;

    public ControllerHelper(final GigaSpace gigaSpace, final Admin admin) {
        this.gigaSpace = gigaSpace;
        this.admin = admin;
    }

    /**
     * Retrieves an application by name.
     * @param appName The application name.
     * @return {@link org.openspaces.admin.application.Application} - The application.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the application is not found.
     */
    public Application getApplication(final String appName) throws ResourceNotFoundException {

        Application application = admin.getApplications().getApplication(appName);
        if (application == null) {
            throw new ResourceNotFoundException(appName);
        }
        return application;

    }

    /**
     * Retrieves a service instance from a processing unit by instance id.
     * @param processingUnit The processing unit.
     * @param instanceId The instance id.
     * @return {@link org.openspaces.admin.pu.ProcessingUnitInstance} - The processing unit instance.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the instance does not exist.
     */
    public ProcessingUnitInstance getServiceInstance(final ProcessingUnit processingUnit,
                                                        final int instanceId) throws ResourceNotFoundException {

        ProcessingUnitInstance pui = null;
        for (ProcessingUnitInstance processingUnitInstance : processingUnit.getInstances()) {
            if (processingUnitInstance.getInstanceId() == instanceId) {
                pui = processingUnitInstance;
                break;
            }
        }
        if (pui == null) {
            throw new ResourceNotFoundException(processingUnit.getName() + "[" + instanceId + "]");
        }
        return pui;
    }

    /**
     * Retrieves a service by application name and service name.
     * @param appName The application name.
     * @param serviceName The service name.
     * @return {@link org.openspaces.admin.pu.ProcessingUnit} - The service.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the service is not found.
     */
    public ProcessingUnit getService(final String appName,
                                        final String serviceName) throws ResourceNotFoundException {

        String absolutePUName = ServiceUtils.getAbsolutePUName(appName, serviceName);
        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePUName);
        if (processingUnit == null) {
            throw new ResourceNotFoundException(ServiceUtils.getAbsolutePUName(appName, serviceName));
        }
        return processingUnit;
    }

    /**
     * Retrieves a service instance environment variable from the JVM's environment.
     * @param serviceInstance The service instance.
     * @param variable The name of the variable.
     * @return the value of the variable.
     */
    public String getServiceInstanceEnvVariable(final ProcessingUnitInstance serviceInstance,
                                                final String variable) {

        if (StringUtils.isNotBlank(variable)) {
            return serviceInstance.getVirtualMachine().getDetails().getEnvironmentVariables().get(variable);
        }
        return null;
    }

    /**
     * Retrieves a service instance by application name, service name, and instance id.
     * @param appName The application name.
     * @param serviceName The service name.
     * @param instanceId The instance id.
     * @return {@link org.openspaces.admin.pu.ProcessingUnitInstance} - The processing unit instance.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the instance does not exist.
     */
    public ProcessingUnitInstance getServiceInstance(final String appName,
                                                     final String serviceName,
                                                     final int instanceId) throws ResourceNotFoundException {
        ProcessingUnit processingUnit = getService(appName, serviceName);
        return getServiceInstance(processingUnit, instanceId);

    }

    /**
     * Retrieves service instance level attributes.
     * @param appName The application name.
     * @param serviceName The service name.
     * @param instanceId The instance id.
     * @return An instance of {@link org.cloudifysource.dsl.rest.response.GetServiceInstanceAttributesResponse}
     * containing all the service instance attributes names and values.
     */
    public Map<String, Object> getAttributes(final String appName,
                                             final String serviceName, final Integer instanceId) {


        final AbstractCloudifyAttribute templateAttribute =
                createCloudifyAttribute(appName, serviceName, instanceId, null, null);

        // read the matching multiple attributes from the space
        final AbstractCloudifyAttribute[] currAttributes = gigaSpace.readMultiple(templateAttribute);

        // create new map for response
        Map<String, Object> attributes = new HashMap<String, Object>();

        // current attribute for application is null
        if (currAttributes == null) {
            // return empty attributes
            return attributes;

        }

        // update attribute object with current attributes
        for (AbstractCloudifyAttribute applicationCloudifyAttribute : currAttributes) {
            if (applicationCloudifyAttribute.getValue() != null) {
                attributes.put(applicationCloudifyAttribute.getKey(),
                        applicationCloudifyAttribute.getValue().toString());
            }
        }

        // return attributes
        return attributes;
    }

    /**
     * Creates a cloudify attribute.
     * @param applicationName The application name.
     * @param serviceName The service name.
     * @param instanceId The instance id.
     * @param name The attribute name.
     * @param value The attribute value.
     * @return the attribute.
     */
    public AbstractCloudifyAttribute createCloudifyAttribute(final String applicationName,
                                                                final String serviceName,
                                                                final Integer instanceId,
                                                                final String name,
                                                                final Object value) {
        // global
        if (applicationName == null) {
            return new GlobalCloudifyAttribute(name, value);
        }
        // application
        if (serviceName == null) {
            return new ApplicationCloudifyAttribute(applicationName, name,
                    value);
        }
        // service
        if (instanceId == null) {
            return new ServiceCloudifyAttribute(applicationName, serviceName,
                    name, value);
        }
        // instance
        return new InstanceCloudifyAttribute(applicationName, serviceName,
                instanceId, name, value);
    }

    /**
     * Delete an instance level attribute.
     * @param appName The application name.
     * @param serviceName The service name.
     * @param instanceId The instance id.
     * @param attributeName The attribute name.
     * @return The previous value for this attribute in the response.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the requested service or service instance does not exist.
     * @throws org.cloudifysource.rest.controllers.RestErrorException Thrown in case the requested attribute name is empty.
     */
    public Object deleteAttribute(final String appName,
                                  final String serviceName,
                                  final Integer instanceId,
                                  final String attributeName) throws ResourceNotFoundException, RestErrorException {

        // attribute name is null
        if (StringUtils.isBlank(attributeName)) {
            throw new RestErrorException(CloudifyMessageKeys.EMPTY_ATTRIBUTE_NAME.getName());
        }

        // get attribute template
        final AbstractCloudifyAttribute attributeTemplate =
                createCloudifyAttribute(appName, serviceName, instanceId, attributeName, null);

        // delete value
        final AbstractCloudifyAttribute previousValue = gigaSpace.take(attributeTemplate);

        // not exist attribute name
        if (previousValue == null) {
            throw new ResourceNotFoundException(attributeName);
        }

        // return previous value for attribute that already deleted
        return previousValue.getValue();

    }

    /**
     * Sets service instance level attributes for the given application.
     * @param appName The application name.
     * @param serviceName The service name.
     * @param instanceId The instance id.
     * @param attributesMap specifying the attributes names and values.
     * @throws org.cloudifysource.rest.controllers.RestErrorException Thrown in case the request body is empty.
     * @throws org.cloudifysource.rest.exceptions.ResourceNotFoundException Thrown in case the service instance does not exist.
     */
    public void setAttributes(final String appName,
                              final String serviceName,
                              final Integer instanceId,
                              final Map<String, Object> attributesMap) throws RestErrorException {

        // validate attributes map
        if (attributesMap == null) {
            throw new RestErrorException(CloudifyMessageKeys.EMPTY_REQUEST_BODY_ERROR.getName());
        }

        // create templates attributes to write
        final AbstractCloudifyAttribute[] attributesToWrite = new AbstractCloudifyAttribute[attributesMap.size()];

        int i = 0;
        for (final Map.Entry<String, Object> attrEntry : attributesMap.entrySet()) {
            final AbstractCloudifyAttribute newAttr =
                    createCloudifyAttribute(appName, serviceName, instanceId, attrEntry.getKey(), null);
            gigaSpace.take(newAttr);
            newAttr.setValue(attrEntry.getValue());
            attributesToWrite[i++] = newAttr;
        }
        // write attributes
        gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

}
