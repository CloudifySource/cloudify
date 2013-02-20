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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;
import org.cloudifysource.shell.AbstractAdminFacade;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.rest.ErrorStatusException;
import org.cloudifysource.shell.rest.RestAdminFacade;

import com.j_spaces.kernel.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.jms.IllegalStateException;

/**
 * @author rafi
 * @since 2.0.0
 */
@Command(scope = "cloudify", name = "start-local-cloud", description = "starts a local cloud")
public class StartLocalCloud extends AbstractGSCommand {

	private static final String LINUX_EXECUTABLE = "gs-agent.sh";
	private static final String WINDOWS_EXECUTABLE = "cmd.exe";// /c
																// gs-agent.bat";
	private static final String[] WINDOWS_PREFIX = { "/c", "gs-agent.bat" };
	private static final String[] AGENT_PARAMETERS = { "gsa.global.esm", "1",
			"gsa.gsc", "0", "gsa.global.gsm", "1", "gsa.global.lus", "1" };

	private static final String[] LINUX_SUFFIX = { ">", "/dev/null", "2>",
			"/dev/null" };
	private static final String[] WINDOWS_SUFFIX = { ">", "NUL", "2>", "NUL" };

	@Argument(index = 0, required = false, name = "groups", description = "The Lookup Group be used by the local cloud. Will default to the LOOKUPGROUPS environment variable if not specified")
	private String groups;

	@Override
	protected Object doExecute() throws ErrorStatusException {

		// TODO - scan for open ports - 8080, 8099
		Admin admin = createAdmin();
		boolean lusFound = admin.getLookupServices().waitFor(1, 2,
				TimeUnit.SECONDS);
		if (lusFound) {
			return getFormattedMessage("lus_already_running",
					Arrays.toString(admin.getGroups()));
		}

		// "gs-agent.bat gsa.global.esm 1 gsa.gsc 0 gsa.global.gsm 1 gsa.global.lus 1"
		try {
			startLocalAgent(admin);
		} catch (ExecuteException e) {
			logger.log(Level.SEVERE, "Failed to start local cloud agent", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to start local cloud agent", e);
		}

		try {
			deployRestAdminServer(admin);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to start REST Admin Server", e);
		}

		// connect to local rest server
		if (session != null) {
			this.adminFacade = (AbstractAdminFacade) session
					.get(Constants.ADMIN_FACADE);

		} else {
			this.adminFacade = new RestAdminFacade();
		}
		adminFacade.connect(null, null, "http://localhost:8080/restful");

		try {
			deployWebUI(admin);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to start Web UI for local cloud",
					e);
		}

		return getFormattedMessage("local_cloud_started"); // TODO - add local
															// cloud messages
	}

	private void deployRestAdminServer(Admin admin) throws IOException {
		final String restFileName = "tools" + File.separator + "rest"
				+ File.separator + "rest.war";
		final File restFile = getGSFile(restFileName);

		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				restFile)
				// .memoryCapacityPerContainer(externalProcessMemoryInMB,
				// MemoryUnit.MEGABYTES)
				.addCommandLineArgument("-Xmx" + 64 + "m")
				// .addCommandLineArgument("-Xms" + containerMemoryInMB + "m")
				.addContextProperty("com.gs.application", "management")
				.name("restful")
				// All PUs on this role share the same machine. Machines
				// are identified by zone.
				.sharedMachineProvisioning(
						"public",
						new DiscoveredMachineProvisioningConfigurer()
								.addGridServiceAgentZone("management")
								// .reservedMemoryCapacityPerMachine(reservedMemoryCapacityPerMachineInMB,
								// MemoryUnit.MEGABYTES)
								.create())
				// Eager scale (1 container per machine per PU)
				.scale(new EagerScaleConfigurer()
						.atMostOneContainerPerMachine().create());

		GridServiceManager gsm = admin.getGridServiceManagers()
				.waitForAtLeastOne(5, TimeUnit.SECONDS);
		if (gsm == null) {
			throw new java.lang.IllegalStateException(
					"Could not find a GSM in the local cloud");
		}

		ProcessingUnit pu = gsm.deploy(deployment);
		boolean res = pu.waitFor(1, 30, TimeUnit.SECONDS);
		if (!res) {
			throw new java.lang.IllegalStateException(
					"REST Admin server did not deploy successfully");
		}

		String url = getWebProcessingUnitURL(pu);

		System.out.println("REST Admin server is available at: " + url);

	}

	private String getWebProcessingUnitURL(ProcessingUnit pu) {
		ProcessingUnitInstance pui = pu.getInstances()[0];
		Map<String, ServiceDetails> alldetails = pui
				.getServiceDetailsByServiceId();

		ServiceDetails details = alldetails.get("jee-container");
		String host = details.getAttributes().get("host").toString();
		String port = details.getAttributes().get("port").toString();
		String ctx = details.getAttributes().get("context-path").toString();
		String url = "http://" + host + ":" + port + ctx;
		return url;
	}

	private void startLocalAgent(Admin admin) throws ExecuteException,
			IOException {

		File binDir = new File(Environment.getHomeDirectory(), "bin");
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(binDir);

		CommandLine cmdLine = createCommandLine();

		executor.setExitValue(0);

		executor.execute(cmdLine, new ExecuteResultHandler() {

			@Override
			public void onProcessFailed(ExecuteException e) {
				logger.log(Level.SEVERE,
						"Local Cloud Agent terminated unexpectedly", e);
			}

			@Override
			public void onProcessComplete(int arg0) {
				logger.fine("Local Cloud Agent terminated");

			}
		});

		boolean foundLus = admin.getLookupServices().waitFor(1, 1,
				TimeUnit.MINUTES);
		if (!foundLus) {
			throw new java.lang.IllegalStateException(
					"Local Cloud Lookup Service did not start!");
		}
	}

	private CommandLine createCommandLine() {
		String os = System.getProperty("os.name");
		logger.fine("os.name = " + os);
		if (os == null) {
			throw new java.lang.IllegalStateException(
					"The System Property variable 'os.name' was not set");
		}

		os = os.toLowerCase();
		CommandLine cmdLine = null;
		boolean isWindows = os.startsWith("win");

		if (isWindows) {
			cmdLine = new CommandLine(WINDOWS_EXECUTABLE);
			for (String param : WINDOWS_PREFIX) {
				cmdLine.addArgument(param);
			}

		} else {
			cmdLine = new CommandLine(LINUX_EXECUTABLE);
		}

		for (String param : AGENT_PARAMETERS) {
			cmdLine.addArgument(param);

		}

		if (isWindows) {
			for (String param : WINDOWS_SUFFIX) {
				cmdLine.addArgument(param);
			}
		} else {
			for (String param : LINUX_SUFFIX) {
				cmdLine.addArgument(param);
			}
		}

		return cmdLine;
	}

	private void deployWebUI(Admin admin) throws Exception {
		final String webUIFileName = "tools" + File.separator + "gs-webui"
				+ File.separator + "gs-webui-9.5.0-SNAPSHOT.war";

		File webUIFile = getGSFile(webUIFileName);

		this.adminFacade.installElastic(webUIFile, "management", "web-ui",
				"web-ui", null);

		ProcessingUnit pu = admin.getProcessingUnits().waitFor("web-ui", 10,
				TimeUnit.SECONDS);
		if (pu == null) {
			throw new IllegalStateException(
					"Could not find 'web-ui' processing unit");
		}

		boolean res = pu.waitFor(1, 30, TimeUnit.SECONDS);
		if (!res) {
			throw new java.lang.IllegalStateException(
					"Could not find instance of 'web-ui' processing unit");
		}

		String url = getWebProcessingUnitURL(pu);

		System.out.println("Web UI is available at: " + url);

	}

	private File getGSFile(final String relativeFileName)
			throws FileNotFoundException, IOException {
		final String absoluteFileName = Environment.getHomeDirectory()
				+ File.separator + relativeFileName;
		File file = new File(absoluteFileName);
		if (!file.exists()) {
			throw new FileNotFoundException("File: " + file + " was not found");
		}
		if (!file.isFile()) {
			throw new IOException(file + " is not a file");
		}
		return file;
	}

	private Admin createAdmin() {
		AdminFactory adminFactory = new AdminFactory();
		adminFactory.useDaemonThreads(true);

		if (this.groups != null) {
			adminFactory.addGroups(groups);

		}

		Admin admin = adminFactory.createAdmin();

		return admin;
	}

	public static void main(String[] args) throws ErrorStatusException {
		StartLocalCloud cmd = new StartLocalCloud();

		cmd.doExecute();
	}
}
