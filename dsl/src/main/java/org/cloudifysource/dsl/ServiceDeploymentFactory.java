package org.cloudifysource.dsl;

/**
 * Factory class for creating service deployments easily.
 * @author elip
 *
 */
public class ServiceDeploymentFactory {
	
	public static ServiceDeployment newDedicatedDeplyoment() {
		ServiceDeployment deployment = new ServiceDeployment();
		deployment.setDedicated(new DedicatedServiceDeploymentDescriptor());
		return deployment;
	}

}
