/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.rest.RestLifecycleEventsLatch;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 *        <p/>
 *        This interface defines a set of commands that need to be implemented by an AdminFacade. These commands mainly
 *        deal with the installation of applications and service, execution of custom commands and gathering information
 *        about the current deployment status.
 */
public interface AdminFacade {

	/**
	 * Installs and starts a service on a given application.
	 *
	 * @param applicationName
	 *            The application the service will be deployed in
	 * @param file
	 *            The service file to deploy
	 * @return Response from the server, or null if there was no response.
	 * @throws CLIException
	 *             Reporting a failure to install or start the given service on the specified application
	 */
	String install(String applicationName, File file) throws CLIException;

	/**
	 * Installs the application using the given file, with the specified application name. Contained services are also
	 * installed, ordered according to their dependencies.
	 *
	 * @param applicationFile
	 *            A zip file containing the relevant application files
	 * @param applicationName
	 *            The name of the application
	 * @param authGroups
	 *            A CSV string with names of groups authorized to use this application
	 * @param timeout
	 *            .
	 * @param selfHealing
	 *            True is recipe self healing should be enabled, false otherwise.
	 * @param applicationOverrides
	 *            .
	 * @param cloudOverrides
	 *            .
	 * @return A String-formatted list of the application's services' names, in the required installation order
	 * @throws CLIException
	 *             Reporting a failure to create temporary files or post the file over REST
	 */
	Map<String, String> installApplication(File applicationFile,
			String applicationName, String authGroups, int timeout,
			final boolean selfHealing,
			final File applicationOverrides, File cloudOverrides)
			throws CLIException;

	/**
	 * Installs and starts a service on a given application.
	 *
	 * @param file
	 *            The service file to deploy
	 * @param applicationName
	 *            The name of the application
	 * @param serviceName
	 *            The name of the service
	 * @param zone
	 *            Install in the specified zone
	 * @param props
	 *            Deployment context properties
	 * @param templateName
	 *            The name of the cloud template to use
	 * @param authGroups
	 *            Authorization groups for the service
	 * @param timeout
	 *            .
	 * @param selfHealing
	 *            True is recipe self healing should be enabled, false otherwise.
	 * @param cloudOverrides
	 *            .
	 * @param serviceOverrides
	 *            .
	 * @return String indicating success or failure
	 * @throws CLIException
	 *             Reporting a failure to install the given service
	 */
	String installElastic(File file, String applicationName,
			String serviceName, String zone, Properties props,
			final String templateName, final String authGroups, int timeout,
			final boolean selfHealing, final File cloudOverrides, final File serviceOverrides)
			throws CLIException;

	/**
	 * Installs and starts a service on a given application.
	 *
	 * @param applicationName
	 *            The application the service will be deployed in
	 * @param service
	 *            The service to deploy
	 * @return String indicating success or failure
	 * @throws CLIException
	 *             Reporting a failure to install or start the given service on the specified application
	 */
	String startService(String applicationName, File service)
			throws CLIException;

	/**
	 * Connects to the server, using the given credentials and URL.
	 *
	 * @param user
	 *            The user name, used to create the connection
	 * @param password
	 *            The user name, used to create the connection
	 * @param url
	 *            The URL to connect to
	 * @param isSecureConnection
	 *            is this connection secure (SSL) or not
	 * @throws CLIException
	 *             Reporting a failure to the connect to the server
	 */
	void connect(String user, String password, String url, boolean isSecureConnection) throws CLIException;

	/**
	 * Reconnects to the server, using the given credentials.
	 *
	 * @param username
	 *            The user name, used to create the connection
	 * @param password
	 *            The user name, used to create the connection
	 * @throws CLIException
	 *             Reporting a failure to the connect to the server
	 */
	void reconnect(String username, String password) throws CLIException;

	/**
	 * Verifies the logged in user is a CloudAdmin.
	 *
	 * @throws CLIException
	 *             Reporting a failure to the connect to the server
	 */
	void verifyCloudAdmin() throws CLIException;

	/**
	 * Disconnects from the server.
	 *
	 * @throws CLIException
	 *             Reporting a failure to close the connection to the server
	 */
	void disconnect() throws CLIException;

	/*****
	 * Returns the list of service name of an application.
	 *
	 * @param applicationName
	 *            the application name.
	 * @return the service list.
	 * @throws CLIException .
	 */
	List<String> getServicesList(String applicationName) throws CLIException;

	/**
	 * returns a list of POJOs containing all applications and their description. The description includes deployment
	 * information regarding all of an application's services. and their instances.
	 *
	 * @return List of application description POJOs
	 * @throws CLIException
	 *             Reporting a failure to get the applications list.
	 */
	List<ApplicationDescription> getApplicationDescriptionsList() throws CLIException;

	/**
	 * returns a list of application names.
	 *
	 * @return List of application names
	 * @throws CLIException
	 *             Reporting a failure to get the applications list.
	 */
	List<String> getApplicationNamesList() throws CLIException;

	/**
	 * returns a POJO containing all of an application's services and their description. The description includes
	 * deployment information regarding all of an application's services and their instances.
	 *
	 * @param applicationName
	 *            The application name
	 * @return application description POJO
	 * @throws CLIException
	 *             Reporting a failure to get the services list.
	 */
	ApplicationDescription getServicesDescriptionList(String applicationName) throws CLIException;

	/**
	 * Adds a processing unit instance for the specified service, on the specified application.
	 *
	 * @param applicationName
	 *            The name of the relevant application
	 * @param serviceName
	 *            The name of the service
	 * @param authGroups
	 *            A CSV string of groups authorized to use this application
	 * @param timeout
	 *            The time (number of seconds) this procedure is limited to, before throwing an exception
	 * @throws CLIException
	 *             Reporting a failure to add a processing unit instance of this service
	 */
	void addInstance(String applicationName, String serviceName, String authGroups, int timeout) throws CLIException;

	/**
	 * Remove (undeploy) a specific instance of a given service, on a given application.
	 *
	 * @param applicationName
	 *            The name of the relevant application
	 * @param serviceName
	 *            The name of the service
	 * @param instanceId
	 *            The ID of the instance to remove
	 * @throws CLIException
	 *             Reporting a failure to remove a processing unit instance of this service
	 */
	void removeInstance(String applicationName, String serviceName,
			int instanceId) throws CLIException;

	/**
	 * Indicates if there is a live connection to the server.
	 *
	 * @return connection status (true - connected, false - not connected)
	 * @throws CLIException
	 *             Reporting a failure to query the connectin status
	 */
	boolean isConnected() throws CLIException;

	/**
	 * Undeploys a service, in the context of the given application.
	 *
	 * @param applicationName
	 *            The name of the application the service is currently deployed in
	 * @param serviceName
	 *            The name of the service to undeploy
	 * @param timeoutInMinutes
	 *            .
	 * @return The undeploy response, as a key-value map
	 * @throws CLIException
	 *             Reporting a failure to undeploy the service
	 */
	Map<String, String> undeploy(final String applicationName,
			final String serviceName, int timeoutInMinutes) throws CLIException;

	/**
	 * Returns a Map of deployed instances (name-object) of the given services in the given application.
	 *
	 * @param applicationName
	 *            The name of the application to query for service instances
	 * @param serviceName
	 *            The name of the instances's service
	 * @return A map of deployed instances (name-object)
	 * @throws CLIException
	 *             Reporting a failure to get the instances list.
	 */
	Map<String, Object> getInstanceList(String applicationName,
			String serviceName) throws CLIException;

	/**
	 * Invokes a custom command on all of the specified service instances, on the given application. Custom parameters
	 * are passed as a map using the POST method and contain the command name and parameter values for the specified
	 * command.
	 *
	 * @param applicationName
	 *            The name of the application
	 * @param serviceName
	 *            The name of the service
	 * @param beanName
	 *            Bean name
	 * @param commandName
	 *            Command to execute
	 * @param paramsMap
	 *            The command parameters
	 * @return a Map containing the result of each invocation on a service instance
	 * @throws CLIException
	 *             Reporting a failure to execute the custom command on the specified service
	 */
	Map<String, InvocationResult> invokeServiceCommand(String applicationName,
			String serviceName, String beanName, String commandName,
			Map<String, String> paramsMap) throws CLIException;

	/**
	 * Invokes a custom command on a specific service instance, on the given application. Custom parameters are passed
	 * as a map using the POST method and contain the command name and parameter values for the specified command.
	 *
	 * @param applicationName
	 *            The name of the application
	 * @param serviceName
	 *            The name of the service
	 * @param beanName
	 *            Bean name
	 * @param instanceId
	 *            the ID of the relevant service instance
	 * @param commandName
	 *            Command to execute
	 * @param paramsMap
	 *            The command parameters
	 * @return a Map containing the result of each invocation on a service instance
	 * @throws CLIException
	 *             Reporting a failure to execute the custom command on the specified service
	 */
	InvocationResult invokeInstanceCommand(String applicationName,
			String serviceName, String beanName, int instanceId,
			String commandName, Map<String, String> paramsMap)
			throws CLIException;

	/**
	 * Gets the IP addresses of the machines composing the service grid.
	 *
	 * @return a list of IP addresses
	 * @throws CLIException
	 *             Reporting a failure to get the list of IP addresses
	 */
	List<String> getMachines() throws CLIException;

	/**
	 * Uninstalls the specified application.
	 *
	 * @param applicationName
	 *            The name of the application to uninstall
	 * @param timeoutInMinutes
	 *            .
	 * @return The uninstall response, as a key-value map
	 * @throws CLIException
	 *             Reporting a failure to uninstall the application
	 */
	Map<String, String> uninstallApplication(String applicationName,
			int timeoutInMinutes) throws CLIException;

	/**
	 * Wait for service installation events for a specified time period.
	 *
	 * @param pollingID
	 *            The polling ID for the specific task
	 * @param timeout
	 *            The time (number of minutes) this procedure is limited to, before throwing an exception
	 * @param timeoutMessage
	 *            Timeout message
	 * @throws CLIException
	 *             Thrown in case of a remote rest exception.
	 * @throws InterruptedException
	 *             Reporting the thread is interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	void waitForLifecycleEvents(final String pollingID,
			final int timeout, String timeoutMessage) throws CLIException,
			InterruptedException, TimeoutException;

	/**
	 *
	 * @param applicationName
	 *            .
	 * @param serviceName
	 *            .
	 * @param count
	 *            .
	 * @param locationAware
	 *            .
	 * @param timeout
	 *            .
	 * @return .
	 * @throws CLIException .
	 */
	Map<String, String> setInstances(String applicationName,
			String serviceName, int count, boolean locationAware, int timeout)
			throws CLIException;

	/**
	 *
	 * @param pollingID
	 *            .
	 * @param timeoutMessage
	 *            .
	 * @return .
	 */
	RestLifecycleEventsLatch getLifecycleEventsPollingLatch(
			final String pollingID, String timeoutMessage);

	/**
	 * Retrieves the tail of a service log. This method used the service name and instance id To retrieve the the
	 * instance log tail.
	 *
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param instanceId
	 *            The service instance id.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log of the requested service according to it's instance id.
	 * @throws CLIException
	 *             a CLI exception is thrown if an error occurred on the remote server.
	 */
	String getTailByInstanceId(String serviceName, String applicationName,
			int instanceId, int numLines) throws CLIException;

	/**
	 * Retrieves the tail of a service log. This method used the service name and the instance host address To retrieve
	 * the the instance log tail.
	 *
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param hostAddress
	 *            The service instance's host address.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log of the requested service instance according to it's host address.
	 * @throws CLIException
	 *             a CLI exception is thrown if an error occurred on the remote server.
	 */
	String getTailByHostAddress(String serviceName, String applicationName,
			String hostAddress, int numLines) throws CLIException;

	/**
	 * returns the last n lines of log from each service instance.
	 *
	 * @param serviceName
	 *            The service name.
	 * @param applicationName
	 *            The application name.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log from each service instance.
	 * @throws CLIException
	 *             a CLI exception is thrown if an error occurred on the remote server.
	 */
	String getTailByServiceName(String serviceName, String applicationName,
			int numLines) throws CLIException;

	/**
	 * Update the attribute store with the correct scope.
	 *
	 * @param scope
	 *            A String that represents scope. Can be "global", "application" (which will apply to the current
	 *            application), "service:<service name>" (which will apply to the service with the given name for of the
	 *            given application), or "service:<service name>:<instance id>" (which will apply to the instance with
	 *            the give ID of the given service of the given application)
	 * @param applicationName
	 *            The applicatio name
	 * @param attributes
	 *            the attributes to set in the given scope
	 * @throws CLIException .
	 */
	void updateAttributes(String scope, String applicationName,
			Map<String, String> attributes) throws CLIException;

	/**
	 * List attributes for the given scope.
	 *
	 * @param scope
	 *            A String that represents scope. Can be "global", "application" (which will apply to the current
	 *            application), "service:<service name>" (which will apply to the service with the given name for of the
	 *            given application), or "service:<service name>:<instance id>" (which will apply to the instance with
	 *            the give ID of the given service of the given application)
	 * @param applicationName
	 *            The applicatio name
	 * @return .
	 * @throws CLIException .
	 */
	Map<String, String> listAttributes(String scope, String applicationName)
			throws CLIException;

	/**
	 * Delete attributes for the given scope.
	 *
	 * @param scope
	 *            A String that represents scope. Can be "global", "application" (which will apply to the current
	 *            application), "service:<service name>" (which will apply to the service with the given name for of the
	 *            given application), or "service:<service name>:<instance id>" (which will apply to the instance with
	 *            the give ID of the given service of the given application)
	 * @param applicationName
	 *            The applicatio name
	 * @param attributeNames
	 *            the names of the attributes to delete
	 * @throws CLIException .
	 */
	void deleteAttributes(String scope, String applicationName,
			String... attributeNames) throws CLIException;

	/**
	 *
	 * Adds templates to the cloud. Reads the templates from the (groovy) templates file.
	 *
	 * @param templatesFile
	 *            The groovy templates file.
	 * @return A list of template names that were added to the cloud.
	 * @throws CLIException .
	 */
	List<String> addTemplates(final File templatesFile)
			throws CLIException;

	/**
	 * Lists all cloud's templates.
	 *
	 * @return The cloud's templates.
	 * @throws CLIException .
	 */
	Map<String, ComputeTemplate> listTemplates() 
			throws CLIException;

	/**
	 * Gets a template.
	 *
	 * @param templateName
	 *            The name of the template to get.
	 * @return The template.
	 * @throws CLIException .
	 */
	ComputeTemplate getTemplate(final String templateName) 
			throws CLIException;

	/**
	 * Removes a template.
	 *
	 * @param templateName
	 *            The name of the template to remove.
	 * @throws CLIException .
	 */
	void removeTemplate(final String templateName)
			throws CLIException;

	/**
	 * Returns true if has permissions, else throw exception.
	 *
	 * @param applicationName
	 *            the application name.
	 * @throws CLIException .
	 */
	void hasInstallPermissions(String applicationName)
			throws CLIException;

	/*******
	 * Shuts-down the management processes.
	 *
	 * @return the details of the manager machines.
	 * @throws CLIException .
	 */
	List<ControllerDetails> shutdownManagers() throws CLIException;

	List<ControllerDetails> getManagers() throws CLIException;
}
