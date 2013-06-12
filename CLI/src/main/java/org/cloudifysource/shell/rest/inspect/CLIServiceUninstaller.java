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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.inspect.service.ServiceUninstallationProcessInspector;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/5/13
 * Time: 1:16 AM
 */
public class CLIServiceUninstaller {

    private static final int DEFAULT_TIMEOUT_MINUTES = 5;
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private boolean askOnTimeout = true;
    private String applicationName;
    private String serviceName;
    private RestClient restClient;
    private int initialTimeout;
    private CommandSession session;

    public void setAskOnTimeout(final boolean askOnTimeout) {
        this.askOnTimeout = askOnTimeout;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    public void setRestClient(final RestClient restClient) {
        this.restClient = restClient;
    }

    public void setInitialTimeout(final int initialTimeout) {
        this.initialTimeout = initialTimeout;
    }

    public void setSession(final CommandSession session) {
        this.session = session;
    }

    /**
     * Uninstalls the service.
     * @throws CLIException 
     * @throws InterruptedException 
     * @throws IOException 
     * @throws RestClientException 
     */
    public void uninstall() throws CLIException, InterruptedException, IOException, RestClientException {

        ServiceDescription serviceDescription = 
        		restClient.getServiceDescription(applicationName, serviceName);
        final int nextEventId = getNextEventId(restClient, serviceDescription.getDeploymentId());
        int currentNumberOfRunningInstances = serviceDescription.getInstanceCount();

        restClient.uninstallService(applicationName, serviceName, initialTimeout);


        ServiceUninstallationProcessInspector inspector =
                new ServiceUninstallationProcessInspector(
                        restClient,
                        serviceDescription.getDeploymentId(),
                        false,
                        currentNumberOfRunningInstances,
                        serviceName,
                        applicationName,
                        nextEventId);

        // start polling for life cycle events
        boolean isDone = false;
        displayer.printEvent("uninstalling_service", serviceName);
        displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);

        int actualTimeout = initialTimeout;
        while (!isDone) {
            try {
                inspector.waitForLifeCycleToEnd(actualTimeout);
                isDone = true;
            } catch (final TimeoutException e) {
                // if non interactive, throw exception
                if (!askOnTimeout || !(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
                    throw new CLIException(e.getMessage(), e);
                }

                // ask the user whether to continue viewing the installation or to stop
                displayer.printEvent("");
                boolean continueViewing = promptWouldYouLikeToContinueQuestion();
                if (continueViewing) {
                    // prolong the polling timeouts
                    actualTimeout = DEFAULT_TIMEOUT_MINUTES;
                } else {
                    throw new CLIStatusException(e, "service_uninstallation_timed_out_on_client", serviceName);
                }
            }
        }

        // drop one line before printing the last message
        displayer.printEvent("");
    }

    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_service_uninstallation", serviceName);
    }

    private int getNextEventId(final RestClient client, final String deploymentId) throws RestClientException {
        int lastEventId = 0;
        final DeploymentEvents lastDeploymentEvents = client.getLastEvent(deploymentId);
        if (!lastDeploymentEvents.getEvents().isEmpty()) {
            lastEventId = lastDeploymentEvents.getEvents().iterator().next().getIndex();
        }
        return lastEventId + 1;
    }
}
