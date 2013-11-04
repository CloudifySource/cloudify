/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.rest.response.ShutdownManagementResponse;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author rafi, barakm
 * @since 2.5.0
 * 
 *        Shuts down the managers of the current cloud.
 */
@Command(scope = "cloudify", name = "shutdown-managers", description = "Shuts down the Cloudify manager processes, "
		+ "leaving the hosts up for maintenance. "
		+ "Use bootstrap-cloud -use-existing to restart.")
public class ShutdownManagers extends AbstractGSCommand implements NewRestClientCommand {

	private static final long POLLING_INTERVAL_MILLI_SECONDS = 1000;

	@Option(required = false, name = "-file",
			description = "path to file where controller information will be saved. "
					+ " Can be used to re-bootstrap a cloud.")
	private File existingManagersFile;

	@Option(required = false, name = "-timeout", description = "Minutes to wait for shutdown to complete.")
	private int timeout = 2;

	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	/**
	 * Shuts down the local cloud, and waits until shutdown is complete or until the timeout is reached.
	 * 
	 * @return command return message.
	 * @throws Exception
	 *             if command failed.
	 */
	@Override
	protected Object doExecute() throws Exception {
		validateManagersFilePath();

		if (this.adminFacade == null) {
			adminFacade = getRestAdminFacade();
		}

		if (adminFacade.isConnected()) {
			adminFacade.verifyCloudAdmin();
		} else {
			throw new CLIException(getFormattedMessage(CloudifyErrorMessages.REST_NOT_CONNECTED.getName()));
		}

		final List<ControllerDetails> managers = adminFacade.shutdownManagers();

		final String managerIPs = getManagerIPs(managers);
		logger.info(getFormattedMessage(CloudifyMessageKeys.SHUTDOWN_MANAGERS_INITIATED.getName(), managerIPs));

		writeManagersToFile(managers);

		waitForShutdown(managers, ((RestAdminFacade) adminFacade).getUrl().getPort());
		
		return getFormattedMessage(CloudifyMessageKeys.SHUTDOWN_MANAGERS_SUCCESS.getName());

	}

	private void waitForShutdown(final List<ControllerDetails> managers, final int port)
			throws CLIException, InterruptedException, TimeoutException {

		logger.fine("[waitForShutdown] -  waiting for shutdown of all manaement machines [total " 
			+ managers.size() + " managers]");

		final Set<ControllerDetails> managersStillUp = new HashSet<ControllerDetails>();
		managersStillUp.addAll(managers);

		final ConditionLatch conditionLatch =
				new ConditionLatch()
						.verbose(verbose)
						.pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
						.timeout(timeout, TimeUnit.MINUTES)
						.timeoutErrorMessage(CloudifyErrorMessages.SHUTDOWN_MANAGERS_TIMEOUT.getName());

		conditionLatch.waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				
				final Iterator<ControllerDetails> iterator = managersStillUp.iterator();
				while (iterator.hasNext()) {
					final ControllerDetails manager = iterator.next();
					final String host =
							manager.isBootstrapToPublicIp() ? manager.getPublicIp() : manager.getPrivateIp();
					if (ServiceUtils.isPortFree(host, port)) {
						iterator.remove();
						displayer.printEvent(getFormattedMessage(
								CloudifyErrorMessages.MANAGEMENT_SERVERS_MANAGER_DOWN.getName(), host));
						if (managersStillUp.isEmpty()) {
							logger.fine("all ports are free, disconnecting");
							disconnect();
							return true;
						}
						logger.fine("manager [" + host + "] port is free, " 
								+ managersStillUp.size() + " more to check");
					} else {
						logger.fine("manager [" + host + "] port is not free");
						displayer.printNoChange();
					}
				}
				return false;
			}
		});
	}

	private void writeManagersToFile(final List<ControllerDetails> managers) throws IOException {
		
		logger.fine("[writeManagersToFile] -  writing managers to file [" + existingManagersFile + "]");

		if (this.existingManagersFile != null) {
			final ObjectMapper mapper = new ObjectMapper();
			final String managersAsString = mapper.writeValueAsString(managers);
			FileUtils.writeStringToFile(existingManagersFile, managersAsString);
		}
	}

	private String getManagerIPs(final List<ControllerDetails> managers) {
		List<String> ips = new ArrayList<String>(managers.size());
		for (final ControllerDetails managerDetails : managers) {
			if (managerDetails.isBootstrapToPublicIp()) {
				ips.add(managerDetails.getPublicIp());
			} else {
				ips.add(managerDetails.getPrivateIp());
			}
		}
		return ips.toString();
	}

	private void validateManagersFilePath() {
		if (existingManagersFile != null) {
			if (existingManagersFile.exists() && !existingManagersFile.isFile()) {
				throw new IllegalArgumentException("Expected " + existingManagersFile + " to be a file");
			}
		}
	}

	private void disconnect() {
		try {
			adminFacade.disconnect();
		} catch (final CLIException e) {
			// ignore
		}
		session.put(Constants.ACTIVE_APP, CloudifyConstants.DEFAULT_APPLICATION_NAME);
		GigaShellMain.getInstance().setCurrentApplicationName(CloudifyConstants.DEFAULT_APPLICATION_NAME);

	}

	public File getExistingManagersFile() {
		return existingManagersFile;
	}

	public void setExistingManagersFile(final File existingManagersFile) {
		this.existingManagersFile = existingManagersFile;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {

		validateManagersFilePath();

		adminFacade = getRestAdminFacade();
		if (adminFacade.isConnected()) {
			adminFacade.verifyCloudAdmin();
		} else {
			throw new CLIException(getFormattedMessage(CloudifyErrorMessages.REST_NOT_CONNECTED.getName()));
		}

		final RestAdminFacade rest = (RestAdminFacade) this.adminFacade;
		final RestClient newRestClient = rest.getNewRestClient();

		final ShutdownManagementResponse shutdownManagementResponse = newRestClient.shutdownManagers();
		final List<ControllerDetails> managers = Arrays.asList(shutdownManagementResponse.getControllers());

		final String managerIPs = getManagerIPs(managers);
		logger.info(getFormattedMessage(CloudifyMessageKeys.SHUTDOWN_MANAGERS_INITIATED.getName(), managerIPs));

		writeManagersToFile(managers);
		
		waitForShutdown(managers, rest.getUrl().getPort());

		return getFormattedMessage(CloudifyMessageKeys.SHUTDOWN_MANAGERS_SUCCESS.getName());

	}

}
