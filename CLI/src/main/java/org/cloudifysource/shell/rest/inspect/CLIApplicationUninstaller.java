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

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.inspect.application.ApplicationUninstallationProcessInspector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/5/13
 * Time: 1:17 AM
 */
public class CLIApplicationUninstaller {

    private static final int DEFAULT_TIMEOUT_MINUTES = 15;
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private static final Logger logger = Logger.getLogger(CLIApplicationUninstaller.class.getName());

    private boolean askOnTimeout = true;
    private String applicationName;
    private RestClient restClient;
    private int initialTimeout;
    private CommandSession session;

    public void setAskOnTimeout(final boolean askOnTimeout) {
        this.askOnTimeout = askOnTimeout;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
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
     * Uninstalls the application.
     * @throws RestClientException 
     * @throws CLIException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public void uninstall() throws RestClientException, CLIException, InterruptedException, IOException {

        ApplicationDescription applicationDescription;
    	try {
    		applicationDescription = restClient.getApplicationDescription(applicationName);
            logger.fine("Retrieved application description for application " 
            		+ applicationName + " : " + applicationDescription);
    	} catch (RestClientException e) {
    		if (CloudifyMessageKeys.MISSING_RESOURCE.getName().equals(e.getMessageCode())) {
    			throw MessagesUtils.createRestClientException(
    					e.getVerbose(), 
    					CloudifyErrorMessages.FAILED_TO_LOCATE_APPLICATION.getName(), 
    					applicationName);
//    			throw new RestClientException(CloudifyErrorMessages.FAILED_TO_LOCATE_APPLICATION.getName(), 
//    					"Application " + applicationName + " could not be found", e.getVerbose());
    		}
    		throw e;
    	}
        Map<String, Integer> currentNumberOfRunningInstancesPerService = new HashMap<String, Integer>();
        for (ServiceDescription serviceDescription : applicationDescription.getServicesDescription()) {
            currentNumberOfRunningInstancesPerService.put(
            		serviceDescription.getServiceName(), serviceDescription.getInstanceCount());
        }

        final String deploymentId = applicationDescription.getServicesDescription().get(0).getDeploymentId();
        logger.fine("DeploymentId for application " + applicationName + " is " + deploymentId);

        final int lastEventIndex = restClient.getLastEvent(deploymentId).getIndex();

        ApplicationUninstallationProcessInspector inspector =
                new ApplicationUninstallationProcessInspector(
                        restClient,
                        deploymentId,
                        false,
                        currentNumberOfRunningInstancesPerService,
                        applicationName,
                        lastEventIndex);
        inspector.setServiceDescriptionList(applicationDescription.getServicesDescription());

        displayer.printEvent("uninstalling_application", applicationName);

        restClient.uninstallApplication(
                applicationName, initialTimeout);

        displayer.printEvent("waiting_for_lifecycle_of_application", applicationName);

        // start polling for life cycle events
        boolean isDone = false;
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
                    throw new CLIStatusException(e, "application_uninstallation_timed_out_on_client", applicationName);
                }
            }
        }
        // drop one line before printing the last message
        displayer.printEvent("");
    }

    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_application_uninstallation", applicationName);
    }

    public String getApplicationName() {
        return applicationName;
    }
}
