/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.ServiceProcessingUnit;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.cloudifysource.rest.controllers.ElasticScaleConfigFactory;
import org.cloudifysource.rest.util.IsolationUtils;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;
import org.openspaces.core.util.MemoryUnit;

/**
 * 
 * @author adaml
 * @since 2.6.0
 */
public class ElasticProcessingUnitDeploymentFactoryImpl implements ElasticProcessingUnitDeploymentFactory {
	
	private static final Logger logger = Logger
			.getLogger(ElasticProcessingUnitDeploymentFactoryImpl.class.getName());

	private final String LOCALCLOUD_ZONE = "localcloud";
	
	private DeploymentConfig deploymentDetails;
	
	
	//This should always be set.
	@Override
	public ElasticDeploymentTopology create() {
		return null;
	}
	
//	private void deployDataGrid() throws AdminException,
//			TimeoutException, DSLException, IOException {
//		
//		ServiceProcessingUnit puConfig = deploymentDetails.getPuConfig();
//		final int containerMemoryInMB = dataGridConfig.getSla()
//				.getMemoryCapacityPerContainer();
//		final int maxMemoryInMB = dataGridConfig.getSla()
//				.getMaxMemoryCapacity();
//		final int reservedMemoryCapacityPerMachineInMB = 256;
//
//		logger.finer("received request to install datagrid");
//
//		final ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(
//				serviceName)
//				.memoryCapacityPerContainer(containerMemoryInMB,
//						MemoryUnit.MEGABYTES)
//				.maxMemoryCapacity(maxMemoryInMB, MemoryUnit.MEGABYTES)
//				.addContextProperty(
//						CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
//						applicationName)
//				.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS, authGroups)
//				.highlyAvailable(dataGridConfig.getSla().getHighlyAvailable())
//				// allow single machine for local development purposes
//				.singleMachineDeployment();
//        if (cloud != null) {
//            deployment.addCommandLineArgument("-D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "="
//                    + cloud.getConfiguration().getComponents().getUsm().getPortRange());
//        }
//
//		setContextProperties(deployment, contextProperties);
//
//		if (cloud == null) {
//			if (isLocalCloud()) {
//				setPublicMachineProvisioning(deployment, agentZones,
//						reservedMemoryCapacityPerMachineInMB);
//				deployment.scale(new ManualCapacityScaleConfigurer()
//						.memoryCapacity(
//								dataGridConfig.getSla().getMemoryCapacity(),
//								MemoryUnit.MEGABYTES).create());
//
//			} else {
//				setSharedMachineProvisioning(deployment, agentZones,
//						reservedMemoryCapacityPerMachineInMB);
//				// eager scaling. 1 container per machine
//				deployment.scale(ElasticScaleConfigFactory
//						.createEagerScaleConfig());
//			}
//
//		} else {
//
//			final ComputeTemplate template = getComputeTemplate(cloud,
//					templateName);
//
//			validateAndPrepareStatefulSla(serviceName, dataGridConfig.getSla(),
//					cloud, template);
//
//			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(
//					cloud, template);
//
//			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
//					cloud, template, templateName,
//					this.managementTemplate.getRemoteDirectory(), null);
//			config.setAuthGroups(authGroups);
//
//			if (cloudOverrides != null) {
//				//adaml: commented because of impl change.
////				config.setCloudOverridesPerService(cloudOverrides);
//			}
//
//			final String locators = extractLocators(admin);
//			config.setLocator(locators);
//
//			setDedicatedMachineProvisioning(deployment, config);
//			deployment.memoryCapacityPerContainer(
//					(int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
//
//			// TODO: [itaif] Why only capacity of one container ?
//			deployment.scale(ElasticScaleConfigFactory
//					.createManualCapacityScaleConfig(
//							(int) cloudExternalProcessMemoryInMB, 0,
//							locationAware, true));
//		}
//	}
	
	private ElasticStatelessProcessingUnitDeployment createElasticStatelessUsmDeployment() 
			throws DSLException {
		final ElasticStatelessProcessingUnitDeployment deployment = 
				new ElasticStatelessProcessingUnitDeployment(deploymentDetails.getPackedFile());
		//Shared properties among all deployment types
		addSharedDeploymentParameters(deployment);

		deployment.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL, "true");
		if (!deploymentDetails.getInstallRequest().getSelfHealing()) {
			deployment.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_DISABLE_SELF_HEALING, "false");
		}

		final int containerMemoryInMB = 128;

		deployment
		//.memoryCapacityPerContainer(externalProcessMemoryInMB, MemoryUnit.MEGABYTES)
		.addCommandLineArgument("-Xmx" + containerMemoryInMB + "m")
		.addCommandLineArgument("-Xms" + containerMemoryInMB + "m");

		final Service service = deploymentDetails.getService();
		final boolean scalingRulesDefined = service.getScalingRules() == null;
		if (!isLocalcloud()) {
			boolean dedicated = IsolationUtils.isDedicated(service);

			final CloudifyMachineProvisioningConfig config = createCloudifyMachineProvisioningConfig();

			setIsolationConfig(deployment, dedicated, config);

			long cloudExternalProcessMemoryInMB = 0;

			logger.info("setting deployment memory capacity for container according to isolation method");
			if (dedicated) {
				cloudExternalProcessMemoryInMB = calculateExternalProcessMemoryAccordingToMachineTemplate();
			} else {
				cloudExternalProcessMemoryInMB = IsolationUtils.getInstanceMemoryMB(service);
			}
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			if (scalingRulesDefined) {
				final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createAutomaticCapacityScaleConfig(deploymentDetails.getAbsolutePUName(),
								service, 
								(int) cloudExternalProcessMemoryInMB,
								service.isLocationAware(), 
								dedicated);
				deployment.scale(scaleConfig);
			} else {
				final int totalMemoryInMB = calculateTotalMemoryInMB((int) cloudExternalProcessMemoryInMB);
				final double totalCpuCores = calculateTotalCpuCores(service);
				final ManualCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createManualCapacityScaleConfig(totalMemoryInMB,
								totalCpuCores, service.isLocationAware(), dedicated);
				deployment.scale(scaleConfig);
			}
		} else { //localcloud
			setLocalcloudMachineProvisioningConfig(deployment);
			final int externalProcessMemoryInMB = 512;
			deployment.memoryCapacityPerContainer(externalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			if (scalingRulesDefined) {
				final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createAutomaticCapacityScaleConfig(deploymentDetails.getAbsolutePUName(),
								service, externalProcessMemoryInMB, false, false);
				deployment.scale(scaleConfig);
			} else {
				// the processing unit scales out whenever the specified memory capacity is breached
				final ManualCapacityScaleConfig scaleConfig = createManualScaleConfig(externalProcessMemoryInMB);
				deployment.scale(scaleConfig);
			}
		}
		return deployment;
	}

	private boolean isLocalcloud() {
		if (deploymentDetails.getCloud() == null) {
			return true;
		}
		return false;
	}

	private ElasticStatefulProcessingUnitDeployment createStatefulProcessingUnit() {
		ServiceProcessingUnit puConfig = deploymentDetails.getPuConfig();
		final Sla statefulSla = puConfig.getSla();
		final ElasticStatefulProcessingUnitDeployment deployment = 
								new ElasticStatefulProcessingUnitDeployment(deploymentDetails.getPackedFile());
		//Shared properties among all deployment types
		addSharedDeploymentParameters(deployment);

		//TODO: ask Itai, how is this different from deploying stateful
		final int containerMemoryInMB = statefulSla.getMemoryCapacityPerContainer();
		final int maxMemoryCapacityInMB = statefulSla.getMaxMemoryCapacity();

		deployment.memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES)
					.maxMemoryCapacity(maxMemoryCapacityInMB + "m")
					.highlyAvailable(statefulSla.getHighlyAvailable())
					.singleMachineDeployment();
		
		//TODO:Uncomment this.
//		if (StringUtils.isNotBlank(puConfig.getSpringProfilesActive())) {
//			//note that cloudify uses the SPRING_PROFILES_ACTIVE env variable, but this 
//			//system property overrides it
//			deployment.commandLineArgument("-Dspring.profiles.active=" + puConfig.getSpringProfilesActive());
//		}

		final int memoryCapacity = statefulSla.getMemoryCapacity();
		if (isLocalcloud()) {
			setLocalcloudMachineProvisioningConfig(deployment);
			//setting localcloud scale config
			ManualCapacityScaleConfig manualScaleConfig = createManualScaleConfig(memoryCapacity);
			deployment.scale(manualScaleConfig);

		} else {
			prepareStatefulSla(statefulSla);
			final CloudifyMachineProvisioningConfig config = createCloudifyMachineProvisioningConfig();
			setDedicatedMachineProvisioning(deployment, config);
			//Setting scale config
			final ManualCapacityScaleConfig manualCapacityScaleConfig = ElasticScaleConfigFactory
			.createManualCapacityScaleConfig(memoryCapacity, 0, deploymentDetails.getService().isLocationAware(), true);
			deployment.scale(manualCapacityScaleConfig);
		}
		
		return deployment;
	}
	
	private void prepareStatefulSla(final Sla sla) {

		// Assuming one container per machine then container memory =
		// machine memory
		final int availableMemoryOnMachine = (int) calculateExternalProcessMemoryAccordingToMachineTemplate();
		if (sla.getMemoryCapacityPerContainer() != null
				&& sla.getMemoryCapacityPerContainer() > availableMemoryOnMachine) {
			throw new IllegalStateException(
					"memoryCapacityPerContainer SLA is larger than available memory on machine\n"
							+ sla.getMemoryCapacityPerContainer() + " > "
							+ availableMemoryOnMachine);
		}
		
		if (sla.getMemoryCapacityPerContainer() == null) {
			sla.setMemoryCapacityPerContainer(availableMemoryOnMachine);
		}

		final int minimumNumberOfContainers = sla.getHighlyAvailable() ? 2 : 1;
		final int minMemoryInMB = minimumNumberOfContainers
				* sla.getMemoryCapacityPerContainer();

		if (sla.getMemoryCapacity() == null
				|| sla.getMemoryCapacity() < minMemoryInMB) {

			logger.info("Setting memoryCapacity for service " + deploymentDetails.getAbsolutePUName()
					+ " to minimum " + minMemoryInMB + "MB");
			sla.setMemoryCapacity(minMemoryInMB);
		}

		if (sla.getMaxMemoryCapacity() == null
				|| sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			logger.info("Setting maxMemoryCapacity for service " + deploymentDetails.getAbsolutePUName()
					+ " to memoryCapacity " + sla.getMemoryCapacity() + "MB");
			sla.setMaxMemoryCapacity(sla.getMemoryCapacity());
		}
	}
	
	void setLocalcloudMachineProvisioningConfig(
			final ElasticDeploymentTopology deployment) {
		final int reservedMemoryCapacityPerMachineInMB = 256;
		final int reservedMemoryCapacityPerManagementMachineInMB = 256;
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		final DiscoveredMachineProvisioningConfig config = new DiscoveredMachineProvisioningConfigurer()
				.reservedMemoryCapacityPerMachine(
						reservedMemoryCapacityPerMachineInMB,
						MemoryUnit.MEGABYTES).create();
		
		// localcloud is also the management machine
		config.setReservedMemoryCapacityPerManagementMachineInMB(reservedMemoryCapacityPerManagementMachineInMB);
		String[] agentZones;
		
		//TODO: This should be checked and removed.
		if (isLocalcloud()) {
			agentZones = new String[] { LOCALCLOUD_ZONE };
			config.setGridServiceAgentZones(agentZones);
		}
		
		deployment.publicMachineProvisioning(config);
	}

	CloudifyMachineProvisioningConfig createCloudifyMachineProvisioningConfig() {
		
		final Cloud cloud = deploymentDetails.getCloud();
		final String serviceTemplateName = deploymentDetails.getTemplateName();
		final String managementMachineTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
		final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
		
		final ComputeTemplate serviceTemplate = templates.get(serviceTemplateName);
		final ComputeTemplate managementTemplate = templates.get(managementMachineTemplateName);
		
		String storageTemplate = null;
		if (deploymentDetails.getService().getStorage() != null) {
			storageTemplate = deploymentDetails.getService().getStorage().getTemplate();
			logger.fine("Storage template " + storageTemplate + " is used with deployment");
		}
		logger.info("Creating cloud machine provisioning config. Template remote directory is: "
				+ serviceTemplate.getRemoteDirectory());
		//TODO: is there any reason for passing the remote dir? this could be extracted from the cloud file.
		final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
				deploymentDetails.getCloud(), serviceTemplate, serviceTemplateName, managementTemplate.getRemoteDirectory(),
				storageTemplate);
		config.setAuthGroups(deploymentDetails.getAuthGroups());
		
		String cloudOverrides = deploymentDetails.getCloudOverrides();
		if (cloudOverrides != null) {
			logger.fine("Recieved request for installation of "
					+ deploymentDetails.getAbsolutePUName() + " with cloud overrides parameters [ "
					+ cloudOverrides + "]");
			//TODO: uncomment this.
//			config.setCloudOverridesPerService(cloudOverrides);
		} else {
			logger.fine("No cloud overrides parameters were requested for the installation of "
					+ deploymentDetails.getAbsolutePUName());
		}
		//TODO: When is this used?
		addCloudConfigurationCostants(config);

		config.setLocator(deploymentDetails.getLocators());
		return config;
	}

	private void addCloudConfigurationCostants(
			final CloudifyMachineProvisioningConfig config) {
		File cloudConfigFile = deploymentDetails.getCloudConfigFile();
		if (cloudConfigFile != null) {
			try {
				config.setServiceCloudConfiguration(FileUtils
						.readFileToByteArray(cloudConfigFile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void setIsolationConfig(
			final ElasticStatelessProcessingUnitDeployment deployment,
			final boolean dedicated, final CloudifyMachineProvisioningConfig config) {
		final Service service = deploymentDetails.getService();
		if (IsolationUtils.isUseManagement(service)) {
			config.setDedicatedManagementMachines(false);
		} else {
			config.setDedicatedManagementMachines(true);
		}
		if (dedicated) {
			// service deployment will have a dedicated agent per instance
			setDedicatedMachineProvisioning(deployment, config);
		} else {

			// check what mode of isolation we should use
			if (IsolationUtils.isGlobal(service)) {
				logger.info("global mode is on. will use public machine provisioning for "
						+ deploymentDetails.getAbsolutePUName() + " deployment.");
				logger.info("isolationSLA = " + service.getIsolationSLA());
				// service instances can be deployed across all agents
				deployment.publicMachineProvisioning(config);

			} else if (IsolationUtils.isAppShared(service)) {
				final String applicationName = deploymentDetails.getApplicationName();
				logger.info("app shared mode is on. will use shared machine provisioning for "
						+ deploymentDetails.getAbsolutePUName() + " deployment. isolation id = " + applicationName);
				// service instances can be deployed across all agents with the correct isolation id
				deployment.sharedMachineProvisioning(applicationName, config);
			} else if (IsolationUtils.isTenantShared(service)) {
				String authGroups = deploymentDetails.getAuthGroups();
				if (authGroups == null) {
					throw new IllegalStateException("authGroups cannot be null when using tenant shared isolation");
				}
				logger.info("tenant shared mode is on. will use shared machine provisioning for "
						+ deploymentDetails.getAbsolutePUName() + " deployment. isolation id = " + authGroups);
				// service instances can be deployed across all agents with the correct isolation id
				deployment.sharedMachineProvisioning(authGroups, config);
			}
		}
	}

	//  This is used if service is null or contains no scaling rules.
	ManualCapacityScaleConfig createManualScaleConfig(
			final int externalProcessMemoryInMB) {
		final int totalMemoryInMB = calculateTotalMemoryInMB(externalProcessMemoryInMB);
		final ManualCapacityScaleConfig scaleConfig = new ManualCapacityScaleConfigurer()
		.memoryCapacity(totalMemoryInMB, MemoryUnit.MEGABYTES).create();
		return scaleConfig;
	}
	
	private void setContextProperties(
			final ElasticDeploymentTopology deployment,
			final Properties contextProperties) {
		final Set<Entry<Object, Object>> contextPropsEntries = contextProperties
				.entrySet();
		for (final Entry<Object, Object> entry : contextPropsEntries) {
			deployment.addContextProperty((String) entry.getKey(),
					(String) entry.getValue());
		}
	}
	
	private static double calculateTotalCpuCores(final Service service) {

		if (service == null) { // deploying without a service. assuming CPU requirements is 0
			return 0;
		}

		final double instanceCpuCores = IsolationUtils.getInstanceCpuCores(service);

		if (instanceCpuCores < 0) {
			throw new IllegalArgumentException(
					"instanceCpuCores must be positive");
		}

		final int numberOfInstances = service.getNumInstances();

		return numberOfInstances * instanceCpuCores;
	}
	
	private void setDedicatedMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final ElasticMachineProvisioningConfig config) {
		deployment.dedicatedMachineProvisioning(config);
	}
	
	private long calculateExternalProcessMemoryAccordingToMachineTemplate() {
		String templateName = deploymentDetails.getTemplateName();
		Cloud cloud = deploymentDetails.getCloud();
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(templateName);
		// TODO remove hardcoded number
		logger.info("Calculating external proc mem for template: " + template);
		final int machineMemoryMB = template.getMachineMemoryMB();
		final int reservedMemoryCapacityPerMachineInMB = cloud.getProvider()
				.getReservedMemoryCapacityPerMachineInMB();
		final int safteyMargin = 100; // get rid of this constant. see
		// CLOUDIFY-297
		final long cloudExternalProcessMemoryInMB = machineMemoryMB
				- reservedMemoryCapacityPerMachineInMB - safteyMargin;
		if (cloudExternalProcessMemoryInMB <= 0) {
			throw new IllegalStateException("Cloud template machineMemoryMB ("
					+ machineMemoryMB + "MB) must be bigger than "
					+ "reservedMemoryCapacityPerMachineInMB+" + safteyMargin
					+ " ("
					+ (reservedMemoryCapacityPerMachineInMB + safteyMargin)
					+ ")");
		}
		logger.fine("template.machineMemoryMB = "
				+ template.getMachineMemoryMB() + "MB\n"
				+ "cloud.provider.reservedMemoryCapacityPerMachineInMB = "
				+ reservedMemoryCapacityPerMachineInMB + "MB\n"
				+ "cloudExternalProcessMemoryInMB = "
				+ cloudExternalProcessMemoryInMB + "MB"
				+ "cloudExternalProcessMemoryInMB = cloud.machineMemoryMB - "
				+ "cloud.reservedMemoryCapacityPerMachineInMB" + " = "
				+ cloudExternalProcessMemoryInMB);
		return cloudExternalProcessMemoryInMB;
	}
	
	/**
	 * @param serviceName
	 *            - the absolute name of the service
	 * @param service
	 *            - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB
	 *            - MB memory allocated for the GSC plus the external service.
	 * @return a @{link ManualCapacityScaleConfig} based on the specified service and memory.
	 */
	private int calculateTotalMemoryInMB(final int externalProcessMemoryInMB) {
		if (externalProcessMemoryInMB <= 0) {
			throw new IllegalArgumentException(
					"externalProcessMemoryInMB must be positive");
		}
		int numberOfInstances = deploymentDetails.getService().getNumInstances();
		return externalProcessMemoryInMB * numberOfInstances;
	}
	
	//adds all shared properties among all deployment types
	private void addSharedDeploymentParameters(
			final ElasticDeploymentTopology deployment) {
		deployment.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
														deploymentDetails.getApplicationName())
				  .addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS,
						  									deploymentDetails.getAuthGroups())
				  .name(deploymentDetails.getAbsolutePUName());
        if (!isLocalcloud()) {
            deployment.addCommandLineArgument("-D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "="
                    + deploymentDetails.getCloud().getConfiguration().getComponents().getUsm().getPortRange())
                      .addCommandLineArgument("-Xmx" + deploymentDetails.getCloud().getConfiguration().getComponents()
                    		  .getUsm().getMaxMemory())
                      .addCommandLineArgument("-Xms" + deploymentDetails.getCloud().getConfiguration().getComponents()
                    		  .getUsm().getMinMemory());
        }
		// add context properties
		setContextProperties(deployment, deploymentDetails.getContextProperties());
	}
}
