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
package org.cloudifysource.shell.installer;

import com.j_spaces.kernel.Environment;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.dependency.ProcessingUnitDeploymentDependenciesConfigurer;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.core.GigaSpace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author rafi, barakm
 * @since 2.0.0
 *
 *        Handles the installation of a management space
 *
 */
public class ManagementSpaceServiceInstaller extends AbstractManagementServiceInstaller {

	private boolean highlyAvailable;

	private GigaSpace gigaspace = null;

	private final List<LocalhostBootstrapperListener> eventsListenersList =
			new ArrayList<LocalhostBootstrapperListener>();

	private boolean isLocalcloud;

	private String persistentStoragePath;

	/**
	 * Sets the management space availability behavior. A highly-available space is a space that must always have a
	 * backup instance, running on a separate machine.
	 *
	 * @param highlyAvailable
	 *            High-availability behavior (true - on, false - off)
	 */
	public void setHighlyAvailable(final boolean highlyAvailable) {
		this.highlyAvailable = highlyAvailable;
	}

	@Override
	protected Properties getContextProperties() {

		final Properties props = super.getContextProperties();
		if (this.persistentStoragePath != null) {
			props.setProperty("space.storage.path", this.persistentStoragePath);
		}
		props.setProperty("space.name", this.serviceName);

		return props;
	}

	/**
	 * Installs the management space with the configured settings. If a dependency on another PU is
	 * set, the deployment will wait until at least 1 instance of that PU is available.
	 *
	 * @throws CLIException
	 *             Reporting a failure to get the Grid Service Manager (GSM) to install the service
	 */
	@Override
	public void install() throws ProcessingUnitAlreadyDeployedException, CLIException {

		try {
			if (agentZone == null) {
				throw new IllegalStateException("Management services must be installed on management zone");
			}
			
			final File puFile = getManagementSpacePUFile();
			
			final int numberOfBackups = highlyAvailable ? 1 : 0;
			final ProcessingUnitDeployment deployment =
					new ProcessingUnitDeployment(puFile)
					.name(serviceName)
					.addZone(serviceName)
					.maxInstancesPerMachine(1)
					.partitioned(1, numberOfBackups);
			
			for (final Entry<Object, Object> prop : getContextProperties().entrySet()) {
				deployment.setContextProperty(prop.getKey().toString(), prop.getValue().toString());
			}
			
			for (final String requiredPUName : dependencies) {
				deployment.addDependencies(new ProcessingUnitDeploymentDependenciesConfigurer()
						.dependsOnMinimumNumberOfDeployedInstancesPerPartition(requiredPUName, 1).create());
			}
			
			getGridServiceManager().deploy(deployment);
		} catch (final ProcessingUnitAlreadyDeployedException e) {
			if (isLocalcloud) {
				throw e;
			}
			// this is possible in a re-bootstrap scenario
			logger.warning("Deployment of management space failed because a Processing unit with the "
					+ "same name already exists. If this error occured during recovery of management machines, "
					+ "this error is normal and can be ignored.");
		}
	}

	private File getManagementSpacePUFile() {
		final File puFile = new File(Environment.getHomeDirectory() + "/tools/management-space/management-space.jar");
		if (!puFile.exists() || !puFile.isFile()) {
			throw new IllegalStateException("Expected to find management space jar file at: "
					+ puFile.getAbsolutePath());
		}
		return puFile;
	}

	/**
	 * Waits for the management space installation to completes.
	 *
	 * @param adminFacade
	 *            Admin facade to use for deployment
	 * @param agent
	 *            The grid service agent to use
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The {@link TimeUnit} to use
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 * @throws CLIException
	 *             Reporting a failure to check the installation progress
	 */
	@Override
	public void waitForInstallation(final AdminFacade adminFacade, final GridServiceAgent agent, final long timeout,
			final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
      createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				final Space space = admin.getSpaces().getSpaceByName(serviceName);

            logger.fine("Looking for a space instance that belongs to agent " + agent.getUid());
				if (space != null) {
					final SpaceInstance[] spaceInstances = space.getInstances();
               if (spaceInstances == null || spaceInstances.length == 0) {
                  logger.fine("Did not find any " + serviceName + " instances");
                  return false;
               }
               for (SpaceInstance instance : spaceInstances) {
                  GridServiceAgent instanceAgent = instance.getMachine().getGridServiceAgent();
                  if (instanceAgent != null && agent.getUid().equals(instanceAgent.getUid())) {
                     // we found a space instance on this agent
                     gigaspace = space.getGigaSpace();
                     return true;
                  } else if (instanceAgent != null ) {
                     logger.fine("Found space instance " + instance.getSpaceInstanceName() + " on agent " +
                             instanceAgent.getUid());
                  }
               }
				}

				logger.fine("Connecting to management space.");
				if (verbose) {
					publishEvent("Connecting to management space.");
				}
				return false;
			}
		});

		logger.fine("Management space is available.");
		if (verbose) {
			logger.fine("Management space is available.");
		}
	}
	
	
	@Override
	public void validateManagementService(final Admin admin, final GridServiceAgent agent, final long timeout, 
			final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
		waitForInstallation(null /*adminFacade*/, agent, timeout, timeunit);
	}
	

	/**
	 * Returns the {@link GigaSpace} member used.
	 *
	 * @return the GigaSpace member
	 */
	public GigaSpace getGigaSpace() {
		return this.gigaspace;
	}

	/*******
	 * Add an event listener.
	 *
	 * @param listener
	 *            .
	 */
	public void addListener(final LocalhostBootstrapperListener listener) {
		this.eventsListenersList.add(listener);
	}

	/*******
	 * Add multiple event listeners.
	 *
	 * @param listeners
	 *            .
	 */
	public void addListeners(final List<LocalhostBootstrapperListener> listeners) {
		for (final LocalhostBootstrapperListener listener : listeners) {
			this.eventsListenersList.add(listener);
		}
	}

	private void publishEvent(final String event) {
		for (final LocalhostBootstrapperListener listener : this.eventsListenersList) {
			listener.onLocalhostBootstrapEvent(event);
		}
	}

	public void setIsLocalCloud(final boolean isLocalCloud) {
		this.isLocalcloud = isLocalCloud;
	}

	public String getPersistentStoragePath() {
		return persistentStoragePath;
	}

	public void setPersistentStoragePath(final String persistentStoragePath) {
		this.persistentStoragePath = persistentStoragePath;
	}

}
