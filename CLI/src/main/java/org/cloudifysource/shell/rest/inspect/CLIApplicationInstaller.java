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
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.inspect.application.ApplicationInstallationProcessInspector;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/5/13
 * Time: 1:02 AM
 */
public class CLIApplicationInstaller {

    private static final int DEFAULT_TIMEOUT_MINUTES = 15;
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private boolean askOnTimeout = true;
    private String applicationName;
    private RestClient restClient;
    private Map<String, Integer> plannedNumberOfInstancesPerService;
    private int initialTimeout;
    private CommandSession session;
    private String deploymentId;

    public void setAskOnTimeout(final boolean askOnTimeout) {
        this.askOnTimeout = askOnTimeout;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setRestClient(final RestClient restClient) {
        this.restClient = restClient;
    }

    public void setPlannedNumberOfInstancesPerService(final Map<String, Integer> plannedNumberOfInstancesPerService) {
        this.plannedNumberOfInstancesPerService = plannedNumberOfInstancesPerService;
    }

    public void setInitialTimeout(final int initialTimeout) {
        this.initialTimeout = initialTimeout;
    }

    public void setSession(final CommandSession session) {
        this.session = session;
    }

    public void setDeploymentId(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    /**
     * Installs the application.
     * @throws CLIException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public void install() throws CLIException, InterruptedException, IOException {

        ApplicationInstallationProcessInspector inspector = new ApplicationInstallationProcessInspector(
                restClient,
                deploymentId,
                applicationName,
                false,
                plannedNumberOfInstancesPerService);

        int actualTimeout = initialTimeout;
        boolean isDone = false;
        displayer.printEvent("installing_application", applicationName);
        displayer.printEvent("waiting_for_lifecycle_of_application", applicationName);
        while (!isDone) {
            try {

                inspector.waitForLifeCycleToEnd(actualTimeout);
                isDone = true;

            } catch (final TimeoutException e) {

                // if non interactive, throw exception
                if (!askOnTimeout || !(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
                    throw new CLIException(e.getMessage(), e);
                }

                // ask user if he want to continue viewing the installation.
                displayer.printEvent("");
                boolean continueViewing = promptWouldYouLikeToContinueQuestion();
                if (continueViewing) {
                    // prolong the polling timeouts
                    actualTimeout = DEFAULT_TIMEOUT_MINUTES;
                } else {
                    throw new CLIStatusException(e, "application_installation_timed_out_on_client",
                            applicationName);
                }
            }
        }
    }

    private boolean promptWouldYouLikeToContinueQuestion()
            throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_application_installation",
                this.applicationName);
    }
}
