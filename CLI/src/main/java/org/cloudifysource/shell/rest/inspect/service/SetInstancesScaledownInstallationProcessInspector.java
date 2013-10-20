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
import org.cloudifysource.shell.exceptions.CLIException;

/**
 * Created with IntelliJ IDEA. User: elip Date: 6/4/13 Time: 12:33 PM
 */
public class SetInstancesScaledownInstallationProcessInspector extends ServiceInstallationProcessInspector {

	private final int plannedNumberOfInstances;

	public SetInstancesScaledownInstallationProcessInspector(
			final RestClient restClient,
			final String deploymentId,
			final boolean verbose,
			final String serviceName,
			final int plannedNumberOfInstances,
			final String applicationName,
			final int currentEventIndex,
			final int currentNumberOfInstances) throws CLIException {
		super(restClient,
				deploymentId,
				verbose,
				serviceName,
				plannedNumberOfInstances,
				currentNumberOfInstances,
				applicationName);
		
		setLastEventIndex(currentEventIndex);
		this.plannedNumberOfInstances = plannedNumberOfInstances;

	}

	@Override
	public boolean lifeCycleEnded() throws RestClientException {
		ServiceDescription description = restClient.getServiceDescription(applicationName, serviceName);
		final int numOfInstances = description.getInstanceCount();
		final boolean finished = (numOfInstances == plannedNumberOfInstances);
		return finished;

	}

}
