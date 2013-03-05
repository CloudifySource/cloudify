package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 *  Provides methods usefully for implementation Rest Controller
 * <br></br>
 * e.g.
 * <br></br> 
 * getApplication(appName) get application by given application name
 * 
 * <ul><h3>possible response codes</h3></ul> 
 * <li>200 OK – if action is successful </li>
 * <li>4** - In case of permission problem or illegal URL</li>
 * <li>5** - In case of exception or server error</li>
 * 
 * @throws UnsupportedOperationException
 *             , org.cloudifysource.rest.controllers.RestErrorException
 * 
 * 
 *
 * <h3> Note : </h3>
 * <ul> this class must be thread safe </ul>
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

	// default Admin waiting time 
	protected static final int DEFAULT_ADMIN_WAITING_TIMEOUT = 10;

	@Autowired(required = true)
	protected MessageSource messageSource;

	/**
	 *@throw unsupported operation exception
	 * 
	 * @see throwUnsupported(String msg)
	 */
	protected void throwUnsupported() {
		throwUnsupported("This method has not been implemented yet");
	}

	/**
	 *@throw unsupported operation exception
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
	protected Application getApplication(final String appName, final int timeout,
			final TimeUnit timeUnit) throws RestErrorException {

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
	 * @throws RestErrorException 
	 * @return service instance
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
	 * @param application
	 *            application object
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @return processingUnit
	 * @throws RestErrorException 
	 */
	protected ProcessingUnit getService(final Application application,
			final String appName, final String serviceName) throws RestErrorException {

		// get processingUnit - service for given application name , service
		// name with
		// default waiting timeout
		return getService(application, appName, serviceName,
				DEFAULT_ADMIN_WAITING_TIMEOUT, TimeUnit.SECONDS);

	}

	/**
	 * get service by given application , application name and service name.
	 * 
	 * @param application
	 *            application object
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @param timeout
	 *            timeout
	 * @param timeunit
	 *            time unit
	 * @return processingUnit
	 * @throws RestErrorException 
	 */
	protected ProcessingUnit getService(final Application application,
			final String appName, final String serviceName, final int timeout, final TimeUnit timeunit)
			throws RestErrorException {

		// get processingUnit for given application name , service name
		ProcessingUnit processingUnit = application.getProcessingUnits()
				.waitFor(ServiceUtils.getAbsolutePUName(appName, serviceName),
						timeout, timeunit);
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
	 * get service by given parameters ( application name , service name ).
	 * 
	 * @param appName
	 *            application name
	 * @param serviceName
	 *            service name
	 * @return processingUnit
	 * @throws RestErrorException 
	 */
	protected ProcessingUnit getService(final String appName, final String serviceName)
			throws RestErrorException {

		// get application
		Application application = getApplication(appName);
		// return processingUnit by given parameters
		return getService(application, appName, serviceName);

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
	protected String getServiceInstanceEnvVarible(final ProcessingUnitInstance serviceInstance, final String variable) {
	
		if (StringUtils.isNotBlank(variable)) {
			return serviceInstance.getVirtualMachine().getDetails().getEnvironmentVariables().get(variable);
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
	 * @throws RestErrorException 
	 */
	protected ProcessingUnitInstance getServiceInstance(final String appName,
			final String serviceName, final int instanceId) throws RestErrorException {

		// get application
		Application application = getApplication(appName);
		// return processingUnit by given parameters
		ProcessingUnit processingUnit = getService(application, appName,
				serviceName);
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

}
