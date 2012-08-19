/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
 *******************************************************************************/
package org.cloudifysource.shell;

import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.rest.RestLifecycleEventsLatch;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 *        <p/>
 *        This interface defines a set of commands that need to be implemented by an AdminFacade. These
 *        commands mainly deal with the installation of applications and service, execution of custom commands
 *        and gathering information about the current deployment status.
 */
public interface AdminFacade {

    /**
     * Installs and starts a service on a given application.
     *
     * @param applicationName The application the service will be deployed in
     * @param file            The service file to deploy
     * @return Response from the server, or null if there was no response.
     * @throws CLIException Reporting a failure to install or start the given service on the specified application
     */
    String install(String applicationName, File file) throws CLIException;

    /**
     * Installs the application using the given file, with the specified application name. Contained services
     * are also installed, ordered according to their dependencies.
     *
     * @param applicationFile A zip file containing the relevant application files
     * @param applicationName The name of the application
     * @return A String-formatted list of the application's services' names, in the required installation
     *         order
     * @throws CLIException Reporting a failure to create temporary files or post the file over REST
     */
    Map<String, String> installApplication(File applicationFile, String applicationName, int timeout) throws CLIException;

    /**
     * Installs and starts a service on a given application.
     *
     * @param file            The service file to deploy
     * @param applicationName The name of the application
     * @param serviceName     The name of the service
     * @param zone            Install in the specified zone
     * @param props           Deployment context properties
     * @param templateName    The name of the cloud template to use
     * @return String indicating success or failure
     * @throws CLIException Reporting a failure to install the given service
     */
    String installElastic(File file, String applicationName, String serviceName, String zone, Properties props,
                          final String templateName, int timeout) throws CLIException;

    /**
     * Installs and starts a service on a given application.
     *
     * @param applicationName The application the service will be deployed in
     * @param service         The service to deploy
     * @return String indicating success or failure
     * @throws CLIException Reporting a failure to install or start the given service on the specified application
     */
    String startService(String applicationName, File service) throws CLIException;

    /**
     * Connects to the server, using the given credentials and URL.
     *
     * @param user     The user name, used to create the connection
     * @param password The user name, used to create the connection
     * @param url      The URL to connect to
     * @throws CLIException Reporting a failure to the connect to the server
     */
    void connect(String user, String password, String url) throws CLIException;

    /**
     * Disconnects from the server.
     *
     * @throws CLIException Reporting a failure to close the connection to the server
     */
    void disconnect() throws CLIException;

    /**
     * Gets a list of the installed applications' names.
     *
     * @return A list of the installed applications' names
     * @throws CLIException Reporting a failure to retrieve the list of installed applications from the Rest server
     */
    List<String> getApplicationsList() throws CLIException;

    /**
     * Gets the list of services deployed in the context of the given application.
     *
     * @param applicationName The name of the application to query for the service list.
     * @return A list of service deployed in the context of the given application
     * @throws CLIException Reporting a failure to get the services list.
     */
    List<String> getServicesList(String applicationName) throws CLIException;

    /**
     * Adds a processing unit instance for the specified service, on the specified application.
     *
     * @param applicationName The name of the relevant application
     * @param serviceName     The name of the service
     * @param timeout         The time (number of seconds) this procedure is limited to, before throwing an exception
     * @throws CLIException Reporting a failure to add a processing unit instance of this service
     */
    void addInstance(String applicationName, String serviceName, int timeout) throws CLIException;

    /**
     * Remove (undeploy) a specific instance of a given service, on a given application.
     *
     * @param applicationName The name of the relevant application
     * @param serviceName     The name of the service
     * @param instanceId      The ID of the instance to remove
     * @throws CLIException Reporting a failure to remove a processing unit instance of this service
     */
    void removeInstance(String applicationName, String serviceName, int instanceId) throws CLIException;

    void restart(ComponentType componentType, String componentName, Set<Integer> componentIDs) throws CLIException;

    /**
     * Indicates if there is a live connection to the server.
     *
     * @return connection status (true - connected, false - not connected)
     * @throws CLIException Reporting a failure to query the connectin status
     */
    boolean isConnected() throws CLIException;

    /**
     * Undeploys a service, in the context of the given application.
     *
     * @param applicationName The name of the application the service is currently deployed in
     * @param serviceName     The name of the service to undeploy
     * @return The undeploy response, as a key-value map
     * @throws CLIException Reporting a failure to undeploy the service
     */
    Map<String, String> undeploy(final String applicationName, final String serviceName, int timeoutInMinutes) throws CLIException;

    /**
     * Returns a Map of deployed instances (name-object) of the given services in the given application.
     *
     * @param applicationName The name of the application to query for service instances
     * @param serviceName     The name of the instances's service
     * @return A map of deployed instances (name-object)
     * @throws CLIException Reporting a failure to get the instances list.
     */
    Map<String, Object> getInstanceList(String applicationName, String serviceName) throws CLIException;

    /**
     * Invokes a custom command on all of the specified service instances, on the given application. Custom
     * parameters are passed as a map using the POST method and contain the command name and parameter values
     * for the specified command.
     *
     * @param applicationName The name of the application
     * @param serviceName     The name of the service
     * @param beanName        Bean name
     * @param commandName     Command to execute
     * @param paramsMap       The command parameters
     * @return a Map containing the result of each invocation on a service instance
     * @throws CLIException Reporting a failure to execute the custom command on the specified service
     */
    Map<String, InvocationResult> invokeServiceCommand(String applicationName, String serviceName, String beanName,
                                                       String commandName, Map<String, String> paramsMap) throws CLIException;

    /**
     * Invokes a custom command on a specific service instance, on the given application. Custom parameters
     * are passed as a map using the POST method and contain the command name and parameter values for the
     * specified command.
     *
     * @param applicationName The name of the application
     * @param serviceName     The name of the service
     * @param beanName        Bean name
     * @param instanceId      the ID of the relevant service instance
     * @param commandName     Command to execute
     * @param paramsMap       The command parameters
     * @return a Map containing the result of each invocation on a service instance
     * @throws CLIException Reporting a failure to execute the custom command on the specified service
     */
    InvocationResult invokeInstanceCommand(String applicationName, String serviceName, String beanName,
                                           int instanceId, String commandName, Map<String, String> paramsMap) throws CLIException;

    /**
     * Gets the IP addresses of the machines composing the service grid.
     *
     * @return a list of IP addresses
     * @throws CLIException Reporting a failure to get the list of IP addresses
     */
    List<String> getMachines() throws CLIException;

    /**
     * Uninstalls the specified application.
     *
     * @param applicationName The name of the application to uninstall
     * @return The uninstall response, as a key-value map
     * @throws CLIException Reporting a failure to uninstall the application
     */
    Map<String, String> uninstallApplication(String applicationName, int timeoutInMinutes) throws CLIException;

    /**
     * Wait for service installation events for a specified time period.
     *
     * @param pollingID      The polling ID for the specific task
     * @param timeout        The time (number of minutes) this procedure is limited to, before throwing an exception
     * @param timeoutMessage Timeout message
     * @throws CLIException         Thrown in case of a remote rest exception.
     * @throws InterruptedException Reporting the thread is interrupted while waiting
     * @throws TimeoutException     Reporting the timeout was reached
     */
    public void waitForLifecycleEvents(final String pollingID, final int timeout, String timeoutMessage)
            throws CLIException, InterruptedException, TimeoutException;

    Map<String, String> setInstances(String applicationName, String serviceName, int count, boolean locationAffinity, int timeout) throws CLIException;

    public RestLifecycleEventsLatch getLifecycleEventsPollingLatch(final String pollingID, String timeoutMessage);

    /**
     * Retrieves the tail of a service log. This method used the service name and instance id
     * To retrieve the the instance log tail.
     *
     * @param applicationName 
     * 				The application name.
     * @param serviceName     
     * 				The service name.
     * @param instanceId      
     * 				The service instance id.
     * @param numLines        
     * 				The number of lines to tail.
     * @return 
     * 				The last n lines of log of the requested service according to it's instance id.
     * @throws CLIException
     * 				a CLI exception is thrown if an error occurred on the remote server.
     */
    String getTailByInstanceId(String serviceName, String applicationName, int instanceId,
                               int numLines) throws CLIException;

    /**
     * Retrieves the tail of a service log. This method used the service name and the instance host address
     * To retrieve the the instance log tail.
     *
     * @param applicationName 
     * 				The application name.
     * @param serviceName     
     * 				The service name.
     * @param hostAddress     
     * 				The service instance's host address.
     * @param numLines        
     * 				The number of lines to tail.
     * @return 
     * 				The last n lines of log of the requested service instance according to it's host address.
 	 * @throws CLIException
     * 				a CLI exception is thrown if an error occurred on the remote server.
     */
    String getTailByHostAddress(String serviceName, String applicationName,
                                String hostAddress, int numLines) throws CLIException;
    
    /**
     * returns the last n lines of log from each service instance.
     * @param serviceName
     * 			The service name.
     * @param applicationName
     * 				The application name.
     * @param numLines
     * 				The number of lines to tail.
     * @return
     * 				The last n lines of log from each service instance.
     * @throws CLIException
     * 				a CLI exception is thrown if an error occurred on the remote server.
     */
    String getTailByServiceName(String serviceName, String applicationName,
    		int numLines) throws CLIException;


    /**
     * Update the attribute store with the correct scope.
     *
     * @param scope           A String that represents scope. Can be "global", "application" (which will apply to the current application),
     *                        "service:<service name>" (which will apply to the service with the given name for of the given application),
     *                        or "service:<service name>:<instance id>" (which will apply to the instance with the give ID of the given
     *                        service of the given application)
     * @param applicationName The applicatio name
     * @param attributes      the attributes to set in the given scope
     * @throws CLIException
     */
    void updateAttributes(String scope, String applicationName, Map<String, String> attributes) throws CLIException;

    /**
     * List attributes for the given scope
     *
     * @param scope           A String that represents scope. Can be "global", "application" (which will apply to the current application),
     *                        "service:<service name>" (which will apply to the service with the given name for of the given application),
     *                        or "service:<service name>:<instance id>" (which will apply to the instance with the give ID of the given
     *                        service of the given application)
     * @param applicationName The applicatio name
     * @return
     * @throws CLIException
     */
    Map<String, String> listAttributes(String scope, String applicationName) throws CLIException;

    /**
     * Delete attributes for the given scope
     *
     * @param scope           A String that represents scope. Can be "global", "application" (which will apply to the current application),
     *                        "service:<service name>" (which will apply to the service with the given name for of the given application),
     *                        or "service:<service name>:<instance id>" (which will apply to the instance with the give ID of the given
     *                        service of the given application)
     * @param applicationName The applicatio name
     * @param attributeNames  the names of the attributes to delete
     */
    void deleteAttributes(String scope, String applicationName, String... attributeNames) throws CLIException;

}
