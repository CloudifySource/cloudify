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

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpacePartition;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;


public class ManagementSpaceServiceInstaller extends AbstractManagementServiceInstaller {
	
	private boolean highlyAvailable;
	
	private GigaSpace gigaspace = null;

	public void setHighlyAvailable(boolean highlyAvailable) {
		this.highlyAvailable = highlyAvailable;
	}
	
	public void install() throws ProcessingUnitAlreadyDeployedException, CLIException{
		
		if (zone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}
		
		final ElasticSpaceDeployment deployment = 
			new ElasticSpaceDeployment(serviceName)
			.memoryCapacityPerContainer(memoryInMB, MemoryUnit.MEGABYTES)
			.highlyAvailable(highlyAvailable)
			.numberOfPartitions(1)
			// All PUs on this role share the same machine. Machines
			// are identified by zone.
			.sharedMachineProvisioning(
					"public",
					new DiscoveredMachineProvisioningConfigurer()
							.addGridServiceAgentZone(zone)
							.reservedMemoryCapacityPerMachine(RESERVED_MEMORY_IN_MB, MemoryUnit.MEGABYTES)
							.create())
			// Eager scale (1 container per machine per PU)
			.scale(new EagerScaleConfigurer()
			       .atMostOneContainerPerMachine()
				   .create());

		for (Entry<Object, Object> prop : getContextProperties().entrySet()) {
			deployment.addContextProperty(prop.getKey().toString(),prop.getValue().toString());
		}
		
		getGridServiceManager().deploy(deployment);
		
	}

	@Override
	public void waitForInstallation(AdminFacade adminFacade,
			GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws InterruptedException,
			TimeoutException, CLIException {
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
			
                Space space = admin.getSpaces().getSpaceByName(serviceName);
                if (space != null)
                {
                	SpacePartition partition = space.getPartition(0);
                	if (partition != null && partition.getPrimary() != null) {
                		gigaspace = space.getGigaSpace();
                		return true;
                	}
                }
                
            	logger.log(Level.INFO,"Connecting to management space.");
            	return false;
			}
		});
		
        logger.info("Management space is available.");
	}

	public GigaSpace getGigaSpace() {
		return this.gigaspace;
	}
}
