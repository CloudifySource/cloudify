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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import net.jini.core.lease.Lease;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.AbstractCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ApplicationCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.GlobalCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.InstanceCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ServiceCloudifyAttribute;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.gigaspaces.client.WriteModifiers;

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

public abstract class BaseRestContoller {

	// thread safe
	// @see http://wiki.fasterxml.com/JacksonFAQ for more info.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired(required = true)
	protected Admin admin;

	@GigaSpaceContext(name = "gigaSpace")
	protected GigaSpace gigaSpace;

	// default Admin waiting time
	protected static final int DEFAULT_ADMIN_WAITING_TIMEOUT = 10;

	@Autowired(required = true)
	protected MessageSource messageSource;

	/**
	 * @throw unsupported operation exception
	 * 
	 * @see throwUnsupported(String msg)
	 */
	protected void throwUnsupported() {
		throwUnsupported("This method has not been implemented yet");
	}

	/**
	 * @throw unsupported operation exception
	 * 
	 * @param msg
	 *            message about why is unsupported operation
	 */
	protected void throwUnsupported(final String msg) {
		throw new UnsupportedOperationException(msg);
	}

	/**
	 * get application by given name.
	 * 
	 * @param appName
	 *            application name
	 * @param timeout
	 *            time out
	 * @param timeUnit
	 *            time unit
	 * @return application
	 * @throws RestErrorException
	 *             if not exist application with given name
	 */
	protected Application getApplication(final String appName,
			final int timeout, final TimeUnit timeUnit)
			throws RestErrorException {

		// get application
		Application application = admin.getApplications().waitFor(appName,
				timeout, TimeUnit.SECONDS);

		// throw exception if application is null
		if (application == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.APPLICATION_WAIT_TIMEOUT.getName(),
					appName, timeout, TimeUnit.SECONDS);
		}

		return application;

	}

	/**
	 * get application by given name.
	 * 
	 * @param appName
	 *            application name
	 * @return application Note : time unit in seconds with default timeout
	 *         DEFAULT_ADMIN_WAITING_TIMEOUT seconds
	 * @throws RestErrorException
	 *             if not exist application with given name
	 */
	protected Application getApplication(final String appName)
			throws RestErrorException {

		// get application by given parameters
		return getApplication(appName, DEFAULT_ADMIN_WAITING_TIMEOUT,
				TimeUnit.SECONDS);

	}

	/**
	 * get service instance by given id belonging specific processUnit.
	 * 
	 * @param processingUnit
	 *            processing unit ( service) , required to be not null!
	 * @param instanceId
	 *            instance id
	 * @return service instance
	 * @throws RestErrorException
	 *             when not exist service instance
	 */
	protected ProcessingUnitInstance getServiceInstance(
			final ProcessingUnit processingUnit, final int instanceId)
			throws RestErrorException {

		// returned service instance
		ProcessingUnitInstance pui = null;

		// searching for instance with given id {@code instanceId}
		for (ProcessingUnitInstance processingUnitInstance : processingUnit
				.getInstances()) {

			if (processingUnitInstance.getInstanceId() == instanceId) {
				pui = processingUnitInstance;
				break;
			}
		}

		// not exist
		if (pui == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.MISSING_SERVICE_INSTANCE.getName(),
					processingUnit.getName(), String.valueOf(instanceId));
		}
		// return service instance
		return pui;
	}

	/**
	 * get service by given application , application name and service name.
	 * 
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @return processingUnit
	 * @throws RestErrorException when service not exist 
	 */
	protected ProcessingUnit getService(final String appName,
			final String serviceName) throws RestErrorException {

		// get processingUnit - service for given application name , service
		// name with
		// default waiting timeout
		return getService(appName, serviceName, DEFAULT_ADMIN_WAITING_TIMEOUT,
				TimeUnit.SECONDS);

	}

	/**
	 * get service by given application , application name and service name.
	 * 
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @param timeout
	 *            timeout
	 * @param timeunit
	 *            time unit
	 * @return processingUnit 
	 * @throws RestErrorException when service not exist 
	 */
	protected ProcessingUnit getService(final String appName,
			final String serviceName, final int timeout, final TimeUnit timeunit)
			throws RestErrorException {

		// get processingUnit for given application name , service name
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(
				ServiceUtils.getAbsolutePUName(appName, serviceName), timeout,
				timeunit);
		// throw RestErrorException with given timeout,time unit if
		// processingUnit is null
		if (processingUnit == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.SERVICE_WAIT_TIMEOUT.getName(),
					serviceName, timeout, timeunit);
		}

		return processingUnit;

	}

	/**
	 * get environment variable in service instance machine by given key.
	 * 
	 * @param serviceInstance
	 *            service instance
	 * @param variable
	 *            - Environment variable
	 * @return string represent value of environment variable
	 */
	protected String getServiceInstanceEnvVarible(
			final ProcessingUnitInstance serviceInstance, final String variable) {

		if (StringUtils.isNotBlank(variable)) {
			return serviceInstance.getVirtualMachine().getDetails()
					.getEnvironmentVariables().get(variable);
		}
		return null;
	}

	/**
	 * get service instance.
	 * 
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @param instanceId
	 *            instance id
	 * @return service instance
	 * @throws RestErrorException when service instance not exist 
	 */
	protected ProcessingUnitInstance getServiceInstance(final String appName,
			final String serviceName, final int instanceId)
			throws RestErrorException {

		// return processingUnit by given parameters
		ProcessingUnit processingUnit = getService(appName, serviceName);

		return getServiceInstance(processingUnit, instanceId);

	}

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

	/**
	 * get attributes for application , service or service instance according to
	 * given parameters <br>
	 * </br> e.g. if serviceName,instanceId are null and appName not null then
	 * will get application attributes if instanceId is the only null then
	 * return service attributes.
	 * 
	 * @param appName application name
	 * @param serviceName service name 
	 * @param instanceId instance id 
	 * 
	 * @see createCloudifyAttribute method
	 * @return map represent attributes
	 */
	protected Map<String, Object> getAttributes(final String appName,
			final String serviceName, final Integer instanceId) {

		// create template according to given parameters
		final AbstractCloudifyAttribute templateAttribute = createCloudifyAttribute(
				appName, serviceName, instanceId, null, null);
		// read the matching multiple attributes from the space
		final AbstractCloudifyAttribute[] currAttributes = gigaSpace
				.readMultiple(templateAttribute);

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
	 * create Cloudify attribute accroding to given parameters. e.g
	 * applicationName,serviceName,instanceId are null return
	 * GlobalCloudifyAttribute
	 * 
	 * @param applicationName
	 *            the application name
	 * @param serviceName
	 *            the service name
	 * @param instanceId
	 *            instance id
	 * @param name
	 *            attribute key
	 * @param value
	 *            attribute value
	 * @return cloudify attributes
	 */
	protected AbstractCloudifyAttribute createCloudifyAttribute(
			final String applicationName, final String serviceName,
			final Integer instanceId, final String name, final Object value) {
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
	 * delete attribute by given string represent name of attribute. this method
	 * is for multiple use , delete for given application , service or service
	 * instance attributes.
	 * 
	 * @param appName
	 *            the application name
	 * @param serviceName
	 *            the service name
	 * @param instanceId
	 *            instance id
	 * @param attributeName
	 *            attribute name to delete.
	 * 
	 * @see createCloudifyAttribute
	 * @return Object represent previous value of already deleted attribute
	 * @throws RestErrorException
	 *             rest error exception when attribute name is blank ( empty or
	 *             null )
	 */
	protected Object deleteAttribute(final String appName,
			final String serviceName, final Integer instanceId,
			final String attributeName) throws RestErrorException {

		// attribute name is null
		if (StringUtils.isBlank(attributeName)) {
			throw new RestErrorException(
					CloudifyMessageKeys.EMPTY_ATTRIBUTE_NAME.getName());
		}

		// delete attribute

		// get attribute template
		final AbstractCloudifyAttribute attributeTemplate = createCloudifyAttribute(
				appName, serviceName, instanceId, attributeName, null);

		// delete value
		final AbstractCloudifyAttribute previousValue = gigaSpace
				.take(attributeTemplate);

		// not exist attribute name
		if (previousValue == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.NOT_EXIST_ATTRIBUTE.getName(),
					attributeName);
		}

		// return previous value for attribute that already deleted
		return previousValue != null ? previousValue.getValue() : null;

	}

	/**
	 * 
	 * set attributes by given string array represent names of attributes to set
	 * for given application , service or service instance.
	 * 
	 * @param appName
	 *            the application name
	 * @param serviceName
	 *            the service name
	 * @param serviceInstance
	 *            service instance id
	 * @param attributesMap
	 *            attributes map to set
	 * @throws RestErrorException
	 *             throw rest error exception if attributes map is null
	 * 
	 */
	protected void setAttributes(final String appName,
			final String serviceName, final Integer serviceInstance,
			final Map<String, Object> attributesMap) throws RestErrorException {

		// validate attributes map
		if (attributesMap == null) {

			throw new RestErrorException(
					CloudifyMessageKeys.EMPTY_REQUEST_BODY_ERROR.getName());
		}

		// create templates attributes to write
		final AbstractCloudifyAttribute[] attributesToWrite = new AbstractCloudifyAttribute[attributesMap
				.size()];

		int i = 0;
		for (final Entry<String, Object> attrEntry : attributesMap.entrySet()) {
			// create template attribute with key
			final AbstractCloudifyAttribute newAttr = createCloudifyAttribute(
					appName, serviceName, serviceInstance, attrEntry.getKey(),
					null);
			// delete previous value if exist
			gigaSpace.take(newAttr);
			// set new value
			newAttr.setValue(attrEntry.getValue());
			// add to array
			attributesToWrite[i++] = newAttr;
		}
		// write attributes
		gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER,
				WriteModifiers.UPDATE_OR_WRITE);

	}

}
