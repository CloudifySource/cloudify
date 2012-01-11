/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ConditionLatch.Predicate;
import org.cloudifysource.shell.rest.RestAdminFacade;


@Command(scope = "cloudify", name = "uninstall-application", description = "Uninstalls an application.")
public class UninstallApplication extends AdminAwareCommand {

	private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for application to uninstall";
	
	private String lastMessage;

	@Argument(index = 0, required = true, name = "The name of the application")
	String applicationName;

	@CompleterValues(index = 0)
	public Collection<String> getCompleterValues() {
		try {
			return getRestAdminFacade().getApplicationsList();
		} catch (CLIException e) {
			return new ArrayList<String>();
		}
	}

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. Defaults to 5 minutes.")
	int timeoutInMinutes=5;

	@Option(required = false, name = "-progress", description = "The polling time interval in seconds, used for checking if the operation is done. Defaults to 5 seconds. Use together with the -timeout option")
	int progressInSeconds=5;

	@Override
	protected Object doExecute() throws Exception {
		
		if (!askUninstallConfirmationQuestion()){
			return getFormattedMessage("uninstall_aborted");
		}
		
		// we need to look at all containers since the application already undeployed and we cannot get only the application containers 
		final Set<String> containerIdsOfApplication = ((RestAdminFacade)adminFacade).getGridServiceContainerUidsForApplication(applicationName);
		if (verbose) {
			logger.info("Containers running PUs of application " +applicationName +":"+containerIdsOfApplication);
		}
		
		if (timeoutInMinutes > 0){
			printStatusMessage(containerIdsOfApplication.size(), containerIdsOfApplication.size(), containerIdsOfApplication);
		}
		this.adminFacade.uninstallApplication(this.applicationName);

		if (timeoutInMinutes > 0) {
			createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {

				@Override
				public boolean isDone() throws CLIException {

					Set<String> allContainerIds = ((RestAdminFacade)adminFacade).getGridServiceContainerUids();
					Set<String> remainingContainersForApplication = new HashSet<String>(containerIdsOfApplication);
					remainingContainersForApplication.retainAll(allContainerIds);
					boolean isDone =remainingContainersForApplication.isEmpty();
					if (!isDone) {
						printStatusMessage(remainingContainersForApplication.size(), containerIdsOfApplication.size(), remainingContainersForApplication);
					}
					//TODO: container has already been removed by un-install.
					//printAllServiceEvents();
					return isDone;
				}


			});
		}
        session.put(Constants.ACTIVE_APP, "default");
        GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("application_uninstalled_succesfully", this.applicationName);
	}

	private void printStatusMessage(int remainingApplicationContainers, int allApplicationContainers, Set<String> remainingContainerIDs){
		String message = "Waiting for all service instances to uninstall. "+
		"Currently " + remainingApplicationContainers + " instances of " + allApplicationContainers + " are still running.";
		
    	if (!StringUtils.equals(message, lastMessage)){
    		logger.info(message + (verbose ? " " + remainingContainerIDs : ""));
    		this.lastMessage = message;
    	}
	}

	//returns true if the answer to the question was 'Yes'.
	private boolean askUninstallConfirmationQuestion() throws IOException {
		
		//we skip question if the shell is running a script.
		if ((Boolean)(session.get(Constants.INTERACTIVE_MODE))){
			String confirmationQuestion = getFormattedMessage("application_uninstall_confirmation", applicationName);
			System.out.print(confirmationQuestion);
			System.out.flush();
			PropertiesReader pr = new PropertiesReader(new InputStreamReader(System.in));
			String readLine = pr.readProperty();
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(readLine) ? true : false;

		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}

	//	private void printAllServiceEvents() throws CLIException {
	//		List<String> serviceNames = adminFacade.getServicesList(applicationName);
	//		for (String serviceName : serviceNames) {
	//			adminFacade.printEventLogs(applicationName, serviceName);
	//		}
	//	}

	private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
		new ConditionLatch()
		.timeout(timeout,timeunit)
		.pollingInterval(progressInSeconds, TimeUnit.SECONDS)
		.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
		.verbose(verbose);
	}

}
