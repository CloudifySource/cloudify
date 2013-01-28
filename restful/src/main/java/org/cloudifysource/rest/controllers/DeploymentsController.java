package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import net.jini.core.lease.Lease;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.AbstractCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ApplicationCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.GlobalCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.InstanceCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ServiceCloudifyAttribute;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.SetApplicationAttributesRequest;
import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.interceptors.ApiVersionValidationAndRestResponseBuilderInterceptor;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.gigaspaces.client.WriteModifiers;

/**
 * This controller is responsible for retrieving information about deployments.
 * It is also the entry point for deploying services and application. <br>
 * <br>
 * The response body will always return in a JSON representation of the
 * {@link Response} Object. <br>
 * A controller method may return the {@link Response} Object directly. in this
 * case this return value will be used as the response body. Otherwise, an
 * implicit wrapping will occur. the return value will be inserted into
 * {@code Response#setResponse(Object)}. other fields of the {@link Response}
 * object will be filled with default values. <br>
 * <h1>Important</h1> {@code @ResponseBody} annotations are not permitted. <br>
 * <br>
 * <h1>Possible return values</h1> 200 - OK<br>
 * 400 - controller throws an exception<br>
 * 500 - Unexpected exception<br>
 * <br>
 * 
 * @see {@link ApiVersionValidationAndRestResponseBuilderInterceptor}
 * @author elip
 * @since 2.5.0
 * 
 */

@Controller
@RequestMapping(value = "/{version}/deployments")
public class DeploymentsController {

	private static final int DEFAULT_ADMIN_WAITING_TIMEOUT = 10;

	@Autowired(required = true)
	private MessageSource messageSource;

	@Autowired(required = true)
	private Admin admin;

	@GigaSpaceContext(name = "gigaSpace")
	private GigaSpace gigaSpace;

	private ObjectMapper objectMapper = new ObjectMapper();
	
	/**
	 * 
	 * @param appName
	 * @param serviceName
	 * @return
	 * @throws RestErrorException
	 */
	@RequestMapping(value = "/{appName}/service/{serviceName}/metadata", method = RequestMethod.GET)
	public ServiceDetails getServiceDetails(@PathVariable final String appName,
			@PathVariable final String serviceName) throws RestErrorException {

		Application application = admin.getApplications().waitFor(appName,
				DEFAULT_ADMIN_WAITING_TIMEOUT, TimeUnit.SECONDS);
		if (application == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.APPLICATION_WAIT_TIMEOUT.getName(),
					appName, DEFAULT_ADMIN_WAITING_TIMEOUT, TimeUnit.SECONDS);
		}
		ProcessingUnit processingUnit = application.getProcessingUnits()
				.waitFor(ServiceUtils.getAbsolutePUName(appName, serviceName),
						DEFAULT_ADMIN_WAITING_TIMEOUT, TimeUnit.SECONDS);
		if (processingUnit == null) {
			throw new RestErrorException(
					CloudifyMessageKeys.SERVICE_WAIT_TIMEOUT.getName(),
					serviceName, DEFAULT_ADMIN_WAITING_TIMEOUT,
					TimeUnit.SECONDS);
		}

		ServiceDetails serviceDetails = new ServiceDetails();
		serviceDetails.setName(serviceName);
		Map<String, org.openspaces.pu.service.ServiceDetails> instancesDetails = 
				new HashMap<String, org.openspaces.pu.service.ServiceDetails>();

		for (ProcessingUnitInstance instance : processingUnit.getInstances()) {
			instancesDetails.put(instance.getProcessingUnitInstanceName(),
					instance.getServiceDetailsByServiceId("USM"));
		}
		serviceDetails.setInstancesDetails(instancesDetails);

		return serviceDetails;
	}

	/**
	 * 
	 * @param appName
	 * @param attributesRequest
	 */
	@RequestMapping(value = "/{appName}/attributes", method = RequestMethod.POST)
	public void setApplicationAttributes(@PathVariable final String appName,
			@RequestBody final SetApplicationAttributesRequest attributesRequest) {

		final AbstractCloudifyAttribute[] attributesToWrite = new AbstractCloudifyAttribute[attributesRequest
				.getAttributes().size()];
		int i = 0;
		for (final Entry<String, Object> attrEntry : attributesRequest
				.getAttributes().entrySet()) {
			final AbstractCloudifyAttribute newAttr = createCloudifyAttribute(
					appName, null, null, attrEntry.getKey(), null);
			gigaSpace.take(newAttr);
			newAttr.setValue(attrEntry.getValue());
			attributesToWrite[i++] = newAttr;
		}
		gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER,
				WriteModifiers.UPDATE_OR_WRITE);
	}

	/**
	 * 
	 * @param appName
	 * @param serviceName
	 * @param instanceId
	 * @param attributeName
	 * @return
	 */
	@RequestMapping(value = "/{appName}/service/{serviceName}/instances/{instanceId}/attributes/{attributeName}", method = RequestMethod.DELETE)
	public Response<DeleteServiceInstanceAttributeResponse> deleteServiceInstanceAttribute(
			@PathVariable final String appName,
			@PathVariable final String serviceName,
			@PathVariable final int instanceId,
			@PathVariable final String attributeName) {

		final InstanceCloudifyAttribute attribute = new InstanceCloudifyAttribute(
				appName, serviceName, instanceId, attributeName, null);

		final InstanceCloudifyAttribute previousValue = gigaSpace
				.take(attribute);

		final Object value = previousValue != null ? previousValue.getValue()
				: null;

		DeleteServiceInstanceAttributeResponse deleteServiceInstanceAttributeResponse = 
				new DeleteServiceInstanceAttributeResponse();
		deleteServiceInstanceAttributeResponse.setAttributeName(attributeName);
		deleteServiceInstanceAttributeResponse.setAttributeLastValue(value);

		Response<DeleteServiceInstanceAttributeResponse> response = 
				new Response<DeleteServiceInstanceAttributeResponse>();

		response.setMessageId(CloudifyMessageKeys.ATTRIBUTE_DELETED_SUCCESSFULLY
				.getName());
		response.setMessage(messageSource.getMessage(
				CloudifyMessageKeys.ATTRIBUTE_DELETED_SUCCESSFULLY.getName(),
				new Object[] { attributeName }, Locale.getDefault()));
		response.setResponse(deleteServiceInstanceAttributeResponse);
		return response;
	}

	private AbstractCloudifyAttribute createCloudifyAttribute(
			final String applicationName, final String serviceName,
			final Integer instanceId, final String name, final Object value) {
		if (applicationName == null) {
			return new GlobalCloudifyAttribute(name, value);
		}
		if (serviceName == null) {
			return new ApplicationCloudifyAttribute(applicationName, name,
					value);
		}
		if (instanceId == null) {
			return new ServiceCloudifyAttribute(applicationName, serviceName,
					name, value);
		}
		return new InstanceCloudifyAttribute(applicationName, serviceName,
				instanceId, name, value);
	}

	/**
	 * 
	 * @param response
	 * @param e
	 * @throws IOException
	 */
	@ExceptionHandler(RestErrorException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public void handleExpectedErrors(final HttpServletResponse response,
			final RestErrorException e) throws IOException {

		String messageId = (String) e.getErrorDescription().get("error");
		Object[] messageArgs = (Object[]) e.getErrorDescription().get(
				"error_args");
		String formattedMessage = messageSource.getMessage(messageId,
				messageArgs, Locale.getDefault());

		Response<Void> finalResponse = new Response<Void>();
		finalResponse.setStatus("Failed");
		finalResponse.setMessage(formattedMessage);
		finalResponse.setMessageId(messageId);
		finalResponse.setResponse(null);
		finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

		String responseString = objectMapper.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}

	/**
	 * 
	 * @param response
	 * @param t
	 * @throws IOException
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

		String responseString = objectMapper.writeValueAsString(finalResponse);
		response.getOutputStream().write(responseString.getBytes());
	}
}
