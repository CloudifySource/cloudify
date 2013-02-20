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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.dependency.ProcessingUnitDeploymentDependenciesConfigurer;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpacePartition;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;

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

	private final List<LocalhostBootstrapperListener> eventsListenersList = new ArrayList<LocalhostBootstrapperListener>();

	private boolean isLocalcloud;

	private String lrmiCommandLineArgument = "";

	/**
	 * Sets the management space availability behavior. A highly-available space is a space that must always
	 * have a backup instance, running on a separate machine.
	 * 
	 * @param highlyAvailable
	 *            High-availability behavior (true - on, false - off)
	 */
	public void setHighlyAvailable(final boolean highlyAvailable) {
		this.highlyAvailable = highlyAvailable;
	}
	
	public void installSpace() throws ProcessingUnitAlreadyDeployedException, CLIException {
		if (isLocalcloud) {
			installOnLocalCloud();
		} else {
			install();
		}
	}

	/**
	 * Installs the management space with the configured settings (e.g. memory, scale). If a dependency on
	 * another PU is set, the deployment will wait until at least 1 instance of that PU is available.
	 * 
	 * @throws ProcessingUnitAlreadyDeployedException
	 *             Reporting installation failure because the PU is already installed
	 * @throws CLIException
	 *             Reporting a failure to get the Grid Service Manager (GSM) to install the service
	 */
	@Override
	public void install() throws ProcessingUnitAlreadyDeployedException, CLIException {

		if (agentZone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}

		final ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(serviceName)
				.memoryCapacityPerContainer(memoryInMB, MemoryUnit.MEGABYTES).highlyAvailable(highlyAvailable)
				.numberOfPartitions(1)
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
	 * Installs the management space with the configured settings inside the 
	 * localcloud dedicated management service container. If a dependency on another PU is set,
	 *  the deployment will wait until at least 1 instance of that PU is available.
	 * 
	 * @throws ProcessingUnitAlreadyDeployedException
	 *             Reporting installation failure because the PU is already installed
	 * @throws CLIException
	 *             Reporting a failure to get the Grid Service Manager (GSM) to install the service
	 */
	public void installOnLocalCloud() 
			throws ProcessingUnitAlreadyDeployedException, CLIException {

		if (agentZone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}

		SpaceDeployment deployment = new SpaceDeployment(serviceName).addZone(serviceName);

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
				if (space != null) {
					final SpacePartition partition = space.getPartition(0);
					if (partition != null && partition.getPrimary() != null) {
						gigaspace = space.getGigaSpace();
						return true;
					}
				}
				
				logger.fine("Connecting to management space.");
				if (verbose){
					publishEvent("Connecting to management space.");
				}
				return false;
			}
		});
		
		logger.fine("Management space is available.");
		if (verbose){
			logger.fine("Management space is available.");
		}
	}

	/**
	 * Returns the {@link GigaSpace} member used.
	 * 
	 * @return the GigaSpace member
	 */
	public GigaSpace getGigaSpace() {
		return this.gigaspace;
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
		for (final LocalhostBootstrapperListener listener : this.eventsListenersList) {
			listener.onLocalhostBootstrapEvent(event);
		}
	}

	public void setIsLocalCloud(boolean isLocalCloud) {
		this.isLocalcloud = isLocalCloud;
	}

	public void setLrmiCommandLineArgument(final String lrmiCommandLineArgument) {
		this.lrmiCommandLineArgument  = lrmiCommandLineArgument;
	}
	
}
