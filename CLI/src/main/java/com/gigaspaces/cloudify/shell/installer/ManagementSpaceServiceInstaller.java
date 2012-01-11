package com.gigaspaces.cloudify.shell.installer;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpacePartition;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;

import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.ConditionLatch;
import com.gigaspaces.cloudify.shell.commands.CLIException;

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
