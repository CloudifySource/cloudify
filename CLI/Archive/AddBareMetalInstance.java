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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.openspaces.cloud.installer.AgentlessInstaller;
import org.openspaces.cloud.installer.InstallationDetails;
import org.openspaces.cloud.installer.InstallerException;
import org.cloudifysource.shell.rest.ErrorStatusException;

/**
 * @author barakme
 * @since 2.0.0
 */
@Command(scope = "cloudify", name = "add-machine", description = "Sets up a new host preconfigured for the service zone")
public class AddBareMetalInstance extends AdminAwareCommand {

	private static final String NODES_PROPERTIES_FILE_NAME = "nodes.properties";
	@Argument(index = 0, required = true, name = "service", description = "The service name to add instance to. Press tab to see the list of currently running services")
	private String serviceName;

	@CompleterValues(index = 0)
	public Collection<String> getComponentList() {
		try {
			return adminFacade.getServicesList(getCurrentApplicationName());
		} catch (final ErrorStatusException e) {
			logger.warning("Could not get list of services: " + e.getReasonCode());
			return null;
		}
	}

	private Properties getProperties() throws IOException {

		FileReader reader = null;
		try {
			reader = new FileReader(NODES_PROPERTIES_FILE_NAME);
			final Properties props = new Properties();
			props.load(reader);
			return props;
		} catch (final FileNotFoundException e) {
			try {
				createDefaultPropertiesFile();
			} catch (final IOException ioe) {
				throw new IOException("File " + NODES_PROPERTIES_FILE_NAME
						+ " was not found. Attempted to create default file, but failed: " + e.getMessage(), ioe);
			}

			throw new FileNotFoundException(
					"File: "
							+ NODES_PROPERTIES_FILE_NAME
							+ " was not found. A default file was generated in the local directory. " +
									"Please set the required values before trying again");
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

	}

	private void createDefaultPropertiesFile() throws IOException {

		InputStream is = null;
		FileOutputStream os = null;
		try {

			is = this.getClass().getClassLoader().getResourceAsStream("META-INF/nodes/default_nodes_config.properties");
			if (is == null) {
				throw new IOException(
						"Failed to create properties file as default properties file was not found on the classpath");
			}
			os = new FileOutputStream(NODES_PROPERTIES_FILE_NAME);
			while (true) {
				final byte[] buff = new byte[10 * 1024];
				final int howmany = is.read(buff);
				os.write(buff, 0, howmany);
				if (howmany < buff.length) {
					return;
				}

			}

		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}

	}

	@Override
	protected Object doExecute() throws ErrorStatusException, IOException {

		final Properties props = getProperties();
		logger.info("Scanning for machine");
		final String machine = chooseMachine(props);

		if (machine == null) {
			return "No machine available for scale out";
		}

		logger.info("Selected machine: " + machine);
		final AgentlessInstaller installer = new AgentlessInstaller();
		final InstallationDetails details = createInstallationDetails(props, machine);

		try {
			installer.installOnMachineWithIP(details, 5, TimeUnit.MINUTES);
		} catch (final TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InstallerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "Machine " + machine + " started up successfully in zone: " + this.serviceName;
	}

	protected InstallationDetails createInstallationDetails(final Properties props, final String machine) {
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(props.getProperty("localDir", "C:/docBaseBare"));

		details.setLocator(props.getProperty("lookupGroups", System.getenv("LOOKUPGROUPS")));
		details.setLus(false);
		details.setPassword(props.getProperty("password"));
		details.setPrivateIp(machine);
		details.setRemoteDir(props.getProperty("remoteDir", "/tmp/gs-files"));
		details.setTargetIP(machine);
		details.setUsername(props.getProperty("username"));
		details.setZones(this.serviceName);
		return details;
	}

	private String chooseMachine(final Properties props) throws UnknownHostException, ErrorStatusException {
		final String nodesString = props.getProperty("nodes", "");
		final String[] nodes = nodesString.split(",");
		final Set<String> activeNodes = getAllActiveNodes();
		for (final String node : nodes) {
			if (node.trim().length() > 0) {
				if (isMachineAvailable(node, activeNodes)) {
					return node;
				}
			}
		}

		return null;
	}

	private boolean isMachineAvailable(final String node, final Set<String> activeNodes) throws UnknownHostException,
			ErrorStatusException {

		final Set<String> nodeNames = new HashSet<String>();
		nodeNames.add(node);
		final InetAddress address = InetAddress.getByName(node);
		nodeNames.add(address.getHostAddress());
		// nodeNames.add(address.getCanonicalHostName());
		nodeNames.add(address.getHostName());

		final int sizeBefore = nodeNames.size();
		nodeNames.removeAll(activeNodes);
		final int sizeAfter = nodeNames.size();

		if (sizeAfter != sizeBefore) {
			return false;
		}

		// check for management machine
		// if (AgentlessInstaller.checkConnection(node, 4166, 1)) {
		// return false;
		// }

		// check for ssh connection

		try {
			AgentlessInstaller.checkConnection(node, 22, 10, TimeUnit.SECONDS);
		} catch (final Exception e) {
			logger.info("Failed connection test on port 22 for machine: " + node);
			return false;
		}

		return true;

	}

	protected Set<String> getAllActiveNodes() throws ErrorStatusException {
		final List<String> machines = this.adminFacade.getMachines();
		final HashSet<String> res = new HashSet<String>();
		res.addAll(machines);
		return res;
		// final Set<Object> activeNodes = new HashSet<Object>();
		// final List<String> apps = this.adminFacade.getApplicationsList();
		// for (final String app : apps) {
		// final List<String> services = this.adminFacade.getServicesList(app);
		// for (final String service : services) {
		// final Map<String, Object> instances =
		// this.adminFacade.getInstanceList(app, service);
		// final Set<Entry<String, Object>> entries = instances.entrySet();
		// for (final Entry<String, Object> entry : entries) {
		// activeNodes.add(entry.getValue());
		// }
		//
		// }
		// }
		// return activeNodes;
	}

}
