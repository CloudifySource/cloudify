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
 ******************************************************************************/
package org.cloudifysource.shell.rest.inspect.application;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.rest.inspect.UninstallationProcessInspector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 9:08 PM
 */
public class ApplicationUninstallationProcessInspector extends UninstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Application un-installation timed out. "
            + "Configure the timeout using the -timeout flag.";


    private List<ServiceDescription> serviceDescriptionList;

    public ApplicationUninstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final boolean verbose,
            final Map<String, Integer> currentRunningInstancesPerService,
            final String applicationName,
            final int nextEventId) {
        super(restClient, deploymentId, applicationName, verbose, 
        		initWithZeros(currentRunningInstancesPerService.keySet()), currentRunningInstancesPerService);
        setLastEventIndex(nextEventId);
    }

    public void setServiceDescriptionList(final List<ServiceDescription> serviceDescriptionList) {
        this.serviceDescriptionList = serviceDescriptionList;
    }

    private static Map<String, Integer> initWithZeros(final Set<String> serviceNames) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String serviceName : serviceNames) {
            map.put(serviceName, 0);
        }
        return map;
    }

    @Override
    public boolean lifeCycleEnded() throws RestClientException {
        // all services before undeploy are still present
        return restClient.getServiceDescriptions(deploymentId).isEmpty();
    }

    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        List<ServiceDescription> servicesDescription = restClient.getServiceDescriptions(deploymentId);
        for (ServiceDescription serviceDescription : servicesDescription) {
            if (serviceDescription.getServiceName().contains(serviceName)) {
                return serviceDescription.getInstanceCount();
            }
        }
        return 0;
    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
