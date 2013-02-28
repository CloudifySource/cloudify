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
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
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
public class ShutdownManagers extends AbstractGSCommand {

	private static final int POLLING_INTERVAL = 1000;

	@Option(required = false, name = "-file",
			description = "path to file where controller information will be saved. "
					+ " Can be used to re-bootstrap a cloud.")
	private File existingManagersFile;

	/**
	 * Shuts down the local cloud, and waits until shutdown is complete or until the timeout is reached.
	 *
	 * @return command return message.
	 * @throws Exception
	 *             if command failed.
	 */
	@Override
	protected Object doExecute() throws Exception {

		if (this.getExistingManagersFile() != null) {
			if (getExistingManagersFile().exists() && !getExistingManagersFile().isFile()) {
				throw new IllegalArgumentException("Expected " + this.getExistingManagersFile() + " to be a file");
			}
		}
		if (this.adminFacade == null) {
			adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
		}

		if (adminFacade.isConnected()) {
			adminFacade.verifyCloudAdmin();
		} else {
			throw new CLIException(getFormattedMessage(CloudifyErrorMessages.REST_NOT_CONNECTED.getName()));
		}

		final List<ControllerDetails> managers = adminFacade.shutdownManagers();
		final StringBuilder sb = new StringBuilder();

		boolean first = true;
		for (final ControllerDetails managerDetails : managers) {

			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			if (managerDetails.isBootstrapToPublicIp()) {
				sb.append(managerDetails.getPublicIp());
			} else {
				sb.append(managerDetails.getPrivateIp());
			}
		}

		final String managerIPs = sb.toString();
		logger.fine("Shutting down: " + managerIPs);
		System.out.println(getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_WAITING_FOR_SHUTDOWN.getName(),
				managerIPs));

		if (this.existingManagersFile != null) {
			final ObjectMapper mapper = new ObjectMapper();
			final String managersAsString = mapper.writeValueAsString(managers);
			FileUtils.writeStringToFile(existingManagersFile, managersAsString);
		}

		final CLIEventsDisplayer displayer = new CLIEventsDisplayer();
		final RestAdminFacade rest = (RestAdminFacade) this.adminFacade;
		final URL url = rest.getUrl();
		final int port = url.getPort();

		final Set<ControllerDetails> managersStillUp = new HashSet<ControllerDetails>();
		managersStillUp.addAll(managers);
		final long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
		while (System.currentTimeMillis() < endTime) {
			Thread.sleep(POLLING_INTERVAL);

			boolean found = false;
			final Iterator<ControllerDetails> iterator = managersStillUp.iterator();

			while (iterator.hasNext()) {
				final ControllerDetails manager = iterator.next();
				final String host = manager.isBootstrapToPublicIp() ? manager.getPublicIp() : manager.getPrivateIp();

				if (ServiceUtils.isPortFree(host, port)) {
					iterator.remove();
					final String msg =
							getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_MANAGER_DOWN.getName(), host);
					displayer.printEvent(msg);
					found = true;
				}

			}

			if (!found) {
				displayer.printNoChange();
			} else {
				if (managersStillUp.isEmpty()) {
					disconnect();
					return getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_SHUTDOWN_SUCCESS.getName());
				}
			}
		}

		throw new CLIException(getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_SHUTDOWN_FAIL.getName()));

	}

	private void disconnect() {
		try {
			adminFacade.disconnect();
		} catch (CLIException e) {
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

}
