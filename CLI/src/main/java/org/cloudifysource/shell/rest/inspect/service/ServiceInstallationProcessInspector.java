/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.rest.inspect.service;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.rest.inspect.InstallationProcessInspector;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 5:38 PM
 * <br></br>
 *
 * Provides functionality for inspecting the installation process of services.
 */
public class ServiceInstallationProcessInspector extends InstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out. "
            + "Configure the timeout using the -timeout flag.";

    protected final String serviceName;

    public ServiceInstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final String serviceName,
                                               final int plannedNumberOfInstances,
                                               final String applicationName) {

        super(
        		restClient, 
        		deploymentId,
        		(applicationName != null ? applicationName : CloudifyConstants.DEFAULT_APPLICATION_NAME),
        		verbose, 
        		createOneEntryMap(serviceName, plannedNumberOfInstances),
        		createOneEntryMap(serviceName, 0));
        this.serviceName = serviceName;
    }

    public ServiceInstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final String serviceName,
                                               final int plannedNumberOfInstances,
                                               final int currentNumberOfInstances,
                                               final String applicationName) {

        super(
        		restClient, 
        		deploymentId,
        		(applicationName != null ? applicationName : CloudifyConstants.DEFAULT_APPLICATION_NAME),
        		verbose, 
        		createOneEntryMap(serviceName, plannedNumberOfInstances) ,
        		createOneEntryMap(serviceName, currentNumberOfInstances));
        this.serviceName = serviceName;
    }

    private static Map<String, Integer> createOneEntryMap(final String serviceName, final int numberOfInstances) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(serviceName, numberOfInstances);
        return map;
    }

    /**
     * Gets the latest events of this deployment id. Events are sorted by event index.
     * @return A list of events. If this is the first time events are requested, all events are retrieved.
     * Otherwise, only new events (that were not reported earlier) are retrieved.
     * @throws RestClientException Indicates a failure to get events from the server.
     */
    @Override
    public boolean lifeCycleEnded() throws RestClientException, CLIException {

    	boolean serviceIsInstalled;
    	try {
            ServiceDescription serviceDescription = restClient
                    .getServiceDescription(applicationName, serviceName);
            logger.fine("Service description is : " + serviceDescription);
            CloudifyConstants.DeploymentState serviceState = serviceDescription.getServiceState();
            if (serviceState.equals(CloudifyConstants.DeploymentState.FAILED)) {
                throw new CLIException(ShellUtils.getFormattedMessage(CloudifyErrorMessages.FAILED_TO_DEPLOY_SERVICE
                        .getName(), serviceName));
            }
            serviceIsInstalled = serviceState.equals(CloudifyConstants.DeploymentState.STARTED);
    	} catch (final RestClientResponseException e) {
    		if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
        		// the service is not available yet
    			serviceIsInstalled = false;
    		} else {
    			throw e;
    		}
    	}

    	return serviceIsInstalled;
    }


    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
    	int instanceCount;
    	try {
    		ServiceDescription serviceDescription = restClient
                .getServiceDescription(applicationName, serviceName);
    		instanceCount = serviceDescription.getInstanceCount();
    	} catch (final RestClientResponseException e) {
    		if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
    			//if we got here - the service is not installed yet
    			instanceCount = 0;
        	} else {
        		throw e;
        	}
    	}

    	return instanceCount;
    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }
}
