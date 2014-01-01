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
 *******************************************************************************/
package org.cloudifysource.shell.rest.inspect;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 7:51 PM
 */
public abstract class UninstallationProcessInspector extends InstallationProcessInspector {

    private final Set<String> cloudResourceNotReleasedServices;
    private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();
    
    public UninstallationProcessInspector(final RestClient restClient,
                                          final String deploymentId,
                                          final String applicationName,
                                          final boolean verbose,
                                          final Map<String, Integer> plannedNumberOfInstancesPerService,
                                          final Map<String, Integer> currentRunningInstancesPerService) {
        super(restClient,
              deploymentId,
              applicationName,
              verbose,
              plannedNumberOfInstancesPerService,
              currentRunningInstancesPerService);
        this.cloudResourceNotReleasedServices = new HashSet<String>(plannedNumberOfInstancesPerService.keySet());
    }

    @Override
    public void waitForLifeCycleToEnd(final long timeout) 
    		throws InterruptedException, CLIException, TimeoutException {

    	printInitialRunningInstances();
    	
        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

        	@Override
        	public boolean isDone() throws CLIException, InterruptedException {
        		try {
        			boolean ended = false;
        			final List<String> latestEvents = getLatestEvents();
        			if (!latestEvents.isEmpty()) {
        				if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
        					ended = true;
        				}
        				displayer.printEvents(latestEvents);
        			} else {
        				displayer.printNoChange();
        			}
        			
					// it is hard to determine exactly when the PU was terminated,
        			// since PUIs are terminated almost instantly after calling the uninstall command.
                    printIfReleasingCloudResources();

        			return ended;
        		} catch (final RestClientException e) {
        			String message = e.getMessageFormattedText();
        			if (message == null) {
        				message = e.getMessage();
        			}
        			throw new CLIException(message, e, e.getVerbose());
        		}
        	}

        	private void printIfReleasingCloudResources() throws RestClientException {

                if (cloudResourceNotReleasedServices.isEmpty()) {
                    // cloud resources release was printed for all services
                    return;
                }

                for (String serviceName : new HashSet<String>(cloudResourceNotReleasedServices)) {
                    try {
                        restClient.getServiceDescription(applicationName, serviceName);
                    } catch (RestClientResponseException e) {
                        if (e.getStatusCode() == CloudifyConstants.HTTP_STATUS_NOT_FOUND) {
                            // if we got here the pu is not longer available even through the zone.
                            // we assume the ESM is currently releasing cloud resources.
                            cloudResourceNotReleasedServices.remove(serviceName);
                            displayer.printEvent(CloudifyConstants.RELEASING_CLOUD_RESOURCES_EVENT + " for service : "
                                    + serviceName);
                        } else {
                            throw e;
                        }
                    }
                }
        }
        });
    }

	private void printInitialRunningInstances() {
		for (Map.Entry<String, Integer> entry : plannedNumberOfInstancesPerService.entrySet()) {
			String serviceName = entry.getKey();
			int numberOfRunningInstances = currentRunningInstancesPerService.get(serviceName);
            displayer.printEvent(serviceName 
            		+ ": Installed " + numberOfRunningInstances + " Planned " + entry.getValue());
		}
	}
}
