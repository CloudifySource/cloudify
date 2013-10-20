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


import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;

/**
 * Created with IntelliJ IDEA. User: elip Date: 6/4/13 Time: 12:33 PM
 */
public class SetInstancesScaleupInstallationProcessInspector extends ServiceInstallationProcessInspector {

	private final int plannedNumberOfInstances;

	public SetInstancesScaleupInstallationProcessInspector(
            final RestClient restClient,
			final String deploymentId,
			final boolean verbose,
			final String serviceName,
			final int plannedNumberOfInstances,
			final String applicationName,
			final int currentEventIndex,
			final int currentNumberOfInstances) {
		super(restClient,
				deploymentId,
				verbose,
				serviceName,
				plannedNumberOfInstances,
                currentNumberOfInstances,
				applicationName);
		this.setLastEventIndex(currentEventIndex);
		this.plannedNumberOfInstances = plannedNumberOfInstances;
	}

	@Override
    public boolean lifeCycleEnded() throws RestClientException {

    	boolean serviceIsInstalled = false;
    	try {
            ServiceDescription serviceDescription = restClient
                    .getServiceDescription(applicationName, serviceName);
            DeploymentState serviceState = serviceDescription.getServiceState();

			serviceIsInstalled = serviceState.equals(CloudifyConstants.DeploymentState.STARTED);
			final boolean numberOfInstancesMet = (serviceDescription.getInstanceCount() == this.plannedNumberOfInstances);
			return serviceIsInstalled && numberOfInstancesMet;

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

}
