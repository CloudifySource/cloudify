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
package org.cloudifysource.shell.rest.inspect.service;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.rest.inspect.UninstallationProcessInspector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: elip Date: 6/4/13 Time: 8:03 PM
 */
public class ServiceUninstallationProcessInspector extends UninstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service un-installation timed out. "
            + "Configure the timeout using the -timeout flag.";


    protected final String serviceName;

	public ServiceUninstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final boolean verbose,
            final int currentNumberOfRunningInstance,
            final String serviceName,
            final String applicationName,
            final int nextEventIndex) {
		super(restClient, deploymentId, applicationName, verbose, createOneEntryMap(serviceName, 0), 
				createOneEntryMap(serviceName, currentNumberOfRunningInstance));
		this.serviceName = serviceName;
		setLastEventIndex(nextEventIndex);
	}
	
	
	private static Map<String, Integer> createOneEntryMap(final String serviceName, final int numberOfInstances) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(serviceName, numberOfInstances);
		return map;
	}

    public String getServiceName() {
        return serviceName;
    }

	@Override
	public boolean lifeCycleEnded() throws RestClientException {
        return restClient.getServiceDescriptions(deploymentId).isEmpty();
	}

	@Override
	public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        List<ServiceDescription> servicesDescription = restClient
                .getServiceDescriptions(deploymentId);
        // there should only be one service
        if (servicesDescription.isEmpty()) {
            return 0;
        }
        if (servicesDescription.size() > 1)  {
            throw new IllegalStateException("Got more than one services for deployment id " + deploymentId);
        }
        return servicesDescription.get(0).getInstanceCount();
	}

	@Override
	public String getTimeoutErrorMessage() {
		return TIMEOUT_ERROR_MESSAGE;
	}
}
