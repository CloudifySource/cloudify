/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.installer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.dependency.ProcessingUnitDeploymentDependenciesConfigurer;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;

import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Handles the installation of a management web service
 * 
 */
public class ManagementWebServiceInstaller extends AbstractManagementServiceInstaller {

	private int port;
	private String username;
	private String password;
	private File warFile;
	private boolean waitForConnection;
	private List<LocalhostBootstrapperListener> eventsListenersList = new ArrayList<LocalhostBootstrapperListener>();
	private boolean isLocalcloud;
	private boolean isSecureConnection; //Indicates whether the connection to this web server is secure (SSL)
	private String lrmiCommandLineArgument = "";

	/**
	 * Sets the service's port.
	 * 
	 * @param port
	 *            The port to be used by the service
	 */
	public void setPort(final int port) {
		this.port = port;
	}
	
	public void setUsername(final String username) {
		this.username = username;
	}
	
	public void setPassword(final String password) {
		this.password = password;
	}

	/**
	 * Sets the service's war file.
	 * 
	 * @param warFile
	 *            The service's war file, required for deployment.
	 */
	public void setWarFile(final File warFile) {
		this.warFile = warFile;
	}
	
	public void installWebService() throws CLIException {
		if (isLocalcloud) {
			installLocalCloud();
		} else {
			install();
		}
	}

	/**
	 * Installs the management web service with the configured settings (e.g. memory, scale). If a dependency
	 * on another PU is set, the deployment will wait until at least 1 instance of that PU is available.
	 * 
	 * @throws ProcessingUnitAlreadyDeployedException
	 *             Reporting installation failure because the PU is already installed
	 * @throws CLIException
	 *             Reporting a failure to get the Grid Service Manager (GSM) to install the service
	 */
	@Override
	public void install() throws CLIException {

		if (agentZone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}

		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				getGSFile(warFile))
				.memoryCapacityPerContainer(memoryInMB, MemoryUnit.MEGABYTES)
				.name(serviceName)
				// All PUs on this role share the same machine. Machines
				// are identified by zone.
				.sharedMachineProvisioning(
						"public",
						new DiscoveredMachineProvisioningConfigurer().addGridServiceAgentZone(agentZone)
								.reservedMemoryCapacityPerMachine(RESERVED_MEMORY_IN_MB, MemoryUnit.MEGABYTES)
								.create())
				// Eager scale (1 container per machine per PU)
				.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create())
				.addCommandLineArgument(this.lrmiCommandLineArgument);

		for (final Entry<Object, Object> prop : getContextProperties().entrySet()) {
			deployment.addContextProperty(prop.getKey().toString(), prop.getValue().toString());
		}

		for (final String requiredPUName : dependencies) {
			deployment.addDependencies(new ProcessingUnitDeploymentDependenciesConfigurer()
					.dependsOnMinimumNumberOfDeployedInstancesPerPartition(requiredPUName, 1).create());
		}
		// The gsc java options define the lrmi port range and memory size if not defined.

		getGridServiceManager().deploy(deployment);
	}
	
	/**
	 * Installs the management web service with the configured settings inside the 
	 * localcloud dedicated management service container. If a dependency on another PU is set,
	 * the deployment will wait until at least 1 instance of that PU is available.
	 * 
	 * @throws ProcessingUnitAlreadyDeployedException
	 *             Reporting installation failure because the PU is already installed
	 * @throws CLIException
	 *             Reporting a failure to get the Grid Service Manager (GSM) to install the service
	 */
	public void installLocalCloud() throws CLIException {

		if (agentZone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}
		
		final ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(
				getGSFile(warFile))
				.addZone(serviceName) 
				.name(serviceName);

		for (final Entry<Object, Object> prop : getContextProperties().entrySet()) {
			deployment.setContextProperty(prop.getKey().toString(), prop.getValue().toString());
		}

		for (final String requiredPUName : dependencies) {
			deployment.addDependencies(new ProcessingUnitDeploymentDependenciesConfigurer()
					.dependsOnMinimumNumberOfDeployedInstancesPerPartition(requiredPUName, 1).create());
		}

		getGridServiceManager().deploy(deployment);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void waitForInstallation(final AdminFacade adminFacade, final GridServiceAgent agent, final long timeout,
			final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
		final long startTime = System.currentTimeMillis();
		final URL url = waitForProcessingUnitInstance(agent, timeout, timeunit);
		final long remainingTime = timeunit.toMillis(timeout) - (System.currentTimeMillis() - startTime);
		if (waitForConnection) {
			waitForConnection(adminFacade, username, password, url, remainingTime, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Indicates it is required to wait for a successful connection with the service after the service
	 * installation completes.
	 */
	public void setWaitForConnection() {
		waitForConnection = true;
	}

	/**
	 * Waits for a PU instance to be available, indicating the service is installed and running. If the
	 * timeout is reached before a connection could be established, a {@link TimeoutException} is thrown.
	 * 
	 * @param agent
	 *            The grid service agent to use
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The {@link TimeUnit} to use
	 * @return URL The URL of the service
	 * @throws InterruptedException
	 *             Reporting the thread is interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the time out was reached
	 * @throws CLIException
	 *             Reporting different errors while creating the connection to the PU
	 */
	public URL waitForProcessingUnitInstance(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			boolean messagePublished = false;
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				logger.fine("Waiting for " + serviceName + " service.");
				if (!messagePublished){
					String message = ShellUtils.getMessageBundle().getString("starting_management_web_service");
					publishEvent(MessageFormat.format(message, serviceName.toUpperCase()));
					messagePublished = true;
				}
				final ProcessingUnit pu = getProcessingUnit();
				boolean isDone = false;
				if (pu != null) {
					for (final ProcessingUnitInstance instance : pu) {
						if (agent.equals(instance.getGridServiceContainer().getGridServiceAgent())) {
							isDone = true;
							break;
						}
					}
				}
				if (!isDone){
					publishEvent(null);
				}
				return isDone;
			}
		});

		// TODO [noak]: verify this always the correct port (SSL-wise) ?
		final URL url = getWebProcessingUnitURL(agent, getProcessingUnit(), isSecureConnection);
		final String serviceNameCapital = StringUtils.capitalize(serviceName);
		String returnMessage = ShellUtils.getMessageBundle().getString("web_service_available_at");
		logger.fine(returnMessage);
		publishEvent(MessageFormat.format(returnMessage, serviceNameCapital, url));
		return url;
	}

	/**
	 * Waits for a connection to be established with the service. If the timeout is reached before a
	 * connection could be established, a {@link TimeoutException} is thrown.
	 * 
	 * @param adminFacade
	 *            The admin facade used to connect and disconnect from the REST server
	 * @param username
	 *            The username for a secure connection to the rest server
	 * @param password
	 *            The password for a secure connection to the rest server
	 * @param url
	 *            The URL of the service
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The {@link TimeUnit} to use
	 * @throws InterruptedException
	 *             Reporting the thread is interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the time out was reached
	 * @throws CLIException
	 *             Reporting different errors while creating the connection to the service
	 */
	private void waitForConnection(final AdminFacade adminFacade, final String username, final String password,
			final URL url, final long timeout, final TimeUnit timeunit)
					throws InterruptedException, TimeoutException,
			CLIException {
		adminFacade.disconnect();
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				try {
					adminFacade.connect(username, password, url.toString(), isSecureConnection);
					return true;
				} catch (final CLIException e) {
					if (verbose) {
						logger.log(Level.INFO, "Error connecting to web service [" + serviceName + "].", e);
					}
				}
				logger.log(Level.INFO, "Connecting to web service [" + serviceName + "].");
				return false;
			}
		});
	}

	/**
	 * Writes the URL of the service to the log.
	 * 
	 * @throws CLIException
	 *             Reporting a failure to get the host address
	 */
	public void logServiceLocation() throws CLIException {
		try {
			final String serviceNameCapital = StringUtils.capitalize(serviceName);
			final String localhost = Constants.getHostAddress();
			logger.info(serviceNameCapital + " service will be available at: http://" + localhost + ":" + port);
		} catch (final UnknownHostException e) {
			throw new CLIException("Failed getting host address", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Properties getContextProperties() {
		final Properties props = super.getContextProperties();
		props.setProperty("web.port", String.valueOf(port));
		props.setProperty("web.context", "/");
		props.setProperty("web.context.unique", "true");
		return props;
	}

	//TODO:consider delete.
	/**
	 * Waits for the management processes (GSM and ESM) to be available. If the timeout is reached before a
	 * connection could be established, a {@link TimeoutException} is thrown.
	 * 
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The {@link TimeUnit} to use
	 * @throws InterruptedException
	 *             Reporting the thread is interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the time out was reached
	 * @throws CLIException
	 *             Reporting different errors while sampling the active management processes
	 */
	public void waitForManagers(final long timeout, final TimeUnit timeunit) throws InterruptedException,
			TimeoutException, CLIException {

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				boolean isDone = true;
				if (0 == admin.getGridServiceManagers().getSize()) {
					isDone = false;
					if (verbose) {
						logger.fine("Waiting for Grid Service Manager");
						publishEvent("Waiting for Grid Service Manager");
					}
				}

				if (admin.getElasticServiceManagers().getSize() == 0) {
					isDone = false;
					if (verbose) {
						logger.fine("Waiting for Elastic Service Manager");
						publishEvent("Waiting for Elastic Service Manager");
					}
				}

				if (!isDone && !verbose) {
					logger.fine("Waiting for Cloudify management processes");
					publishEvent("Waiting for Cloudify management processes");
				}

				return isDone;
			}
		});

		admin.getGridServiceManagers().waitForAtLeastOne();
	}

	private ProcessingUnit getProcessingUnit() {
		return admin.getProcessingUnits().getProcessingUnit(serviceName);
	}

	/**
	 * Constructs and returns the URL of a processing unit instance deployed with the given agent.
	 * 
	 * @param agent
	 *            The agent of the desired processing unit instance
	 * @param pu
	 *            The processing unit to find an instance of, for construction of the requested URL
	 * @return URL to a specific processing unit instance
	 */
	public static URL getWebProcessingUnitURL(final GridServiceAgent agent, final ProcessingUnit pu, 
			final boolean isSecureConnection) {
		ProcessingUnitInstance pui = null;

		for (final ProcessingUnitInstance instance : pu.getInstances()) {
			if (instance.getGridServiceContainer() != null
					&& instance.getGridServiceContainer().getGridServiceAgent() != null
					&& instance.getGridServiceContainer().getGridServiceAgent().equals(agent)) {
				pui = instance;
			}
		}

		if (pui == null) {
			throw new IllegalStateException("Failed finding " + pu.getName() + " on "
					+ agent.getMachine().getHostAddress());
		}

		final Map<String, ServiceDetails> alldetails = pui.getServiceDetailsByServiceId();

		final ServiceDetails details = alldetails.get("jee-container");
		final String host = details.getAttributes().get("host").toString();
		final String port = details.getAttributes().get("port").toString();
		final String ctx = details.getAttributes().get("context-path").toString();
		final String url = ShellUtils.getRestProtocol(isSecureConnection) + "://" + host + ":" + port + ctx;
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			// this is a bug since we formed the URL correctly
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Gets the service's war file. If the war file's path is not an absolute path, it is considered relative
	 * to the home directory.
	 * 
	 * @param warFile
	 *            The service's war file object (possibly with a relative path)
	 * @return The service's war file object
	 */
	public static File getGSFile(final File warFile) {
		File absWarFile = warFile;
		if (!absWarFile.isAbsolute()) {
			absWarFile = new File(Environment.getHomeDirectory(), warFile.getPath());
		}
		return absWarFile;
	}

	public void addListener(LocalhostBootstrapperListener listener) {
		this.eventsListenersList.add(listener);
	}
	
	public void addListeners(List<LocalhostBootstrapperListener> listeners) {
		for (LocalhostBootstrapperListener listener : listeners) {
			this.eventsListenersList.add(listener);
		}
	}
	
	private void publishEvent(final String event) {
		for (final LocalhostBootstrapperListener listner : this.eventsListenersList) {
			listner.onLocalhostBootstrapEvent(event);
		}
	}

	public void setIsLocalCloud(boolean isLocalCloud) {
		this.isLocalcloud = isLocalCloud;
	}
	
	public void setIsSecureConnection(boolean isSecureConnection) {
		this.isSecureConnection = isSecureConnection;
	}
	
	public void setLrmiCommandLineArgument(final String lrmiCommandLineArgument) {
		this.lrmiCommandLineArgument  = lrmiCommandLineArgument;
	}

}
