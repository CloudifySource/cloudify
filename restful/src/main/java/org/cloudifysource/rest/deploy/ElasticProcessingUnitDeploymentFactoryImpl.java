/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.ComputeDetails;
import org.cloudifysource.domain.DataGrid;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.Sla;
import org.cloudifysource.domain.StatefulProcessingUnit;
import org.cloudifysource.domain.StatelessProcessingUnit;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.controllers.ElasticScaleConfigFactory;
import org.cloudifysource.rest.util.IsolationUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;

/**
 * Elastic deployment factory class.
 * 
 * @author adaml
 * @since 2.6.0
 */
public class ElasticProcessingUnitDeploymentFactoryImpl implements ElasticProcessingUnitDeploymentFactory {

	private static final Logger logger = Logger
			.getLogger(ElasticProcessingUnitDeploymentFactoryImpl.class.getName());

	private static final String LOCALCLOUD_ZONE = "localcloud";

	private DeploymentConfig deploymentConfig;

	// this should be used to create temp directories in the shared temp location.
	private final RestConfiguration restConfig;

	public ElasticProcessingUnitDeploymentFactoryImpl(final RestConfiguration restConfig) {
		this.restConfig = restConfig;
	}

	@Override
	public ElasticDeploymentTopology create(final DeploymentConfig deploymentConfig)
			throws ElasticDeploymentCreationException {
		final Service service = deploymentConfig.getService();
		this.deploymentConfig = deploymentConfig;
		if (service.getLifecycle() != null) {
			return createElasticUSMDeployment();
		} else if (service.getStatefulProcessingUnit() != null) {
			return createElasticStatefulProcessingUnit();
		} else if (service.getStatelessProcessingUnit() != null
				|| service.getMirrorProcessingUnit() != null) {
			return createElasticStatelessPUDeployment();
		} else if (service.getDataGrid() != null) {
			return createElasticDatagridDeployment();
		} else {
			throw new ElasticDeploymentCreationException("Unsupported service type");
		}
	}

	private ElasticSpaceDeployment createElasticDatagridDeployment() {

		final String absolutePUName = deploymentConfig.getAbsolutePUName();
		final ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(absolutePUName);
		addSharedDeploymentParameters(deployment);

		final DataGrid dataGrid = deploymentConfig.getService().getDataGrid();
		addProcessingUnitContextProperties(deployment, dataGrid.getContextProperties());
		final Sla puSla = dataGrid.getSla();
		prepareSla(puSla);

		final int containerMemoryInMB = puSla.getMemoryCapacityPerContainer();
		final int maxMemoryInMB = puSla.getMaxMemoryCapacity();
		deployment
				.memoryCapacityPerContainer(containerMemoryInMB,
						MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryInMB, MemoryUnit.MEGABYTES)
				.highlyAvailable(puSla.getHighlyAvailable())
				// allow single machine for local development purposes
				.singleMachineDeployment();
		ManualCapacityScaleConfig scaleConfig;
		if (isLocalcloud()) {
			final Integer memoryCapacity = puSla.getMemoryCapacity();
			scaleConfig = new ManualCapacityScaleConfigurer()
					.memoryCapacity(memoryCapacity,
							MemoryUnit.MEGABYTES).create();
		} else {
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory();
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			scaleConfig = ElasticScaleConfigFactory
					.createManualCapacityScaleConfig((int) cloudExternalProcessMemoryInMB, 0,
							deploymentConfig.getService().isLocationAware(), true);
			// TODO: [itaif] Why only capacity of one container ?
		}
		deployment.scale(scaleConfig);
		return deployment;
	}

	private void addProcessingUnitContextProperties(final ElasticDeploymentTopology deployment,
			final Map<String, String> properties) {
		if (properties != null) {
			for (final Entry<String, String> entry : properties.entrySet()) {
				deployment.addContextProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	// TODO - temp files should be created in shared temp location, as read from restConfig
	private File getBinaryFromPackedRecipe(final File packedService, final String binaryName) throws
			IOException {
		final File tempFile = File.createTempFile(binaryName, "");
		tempFile.delete();
		ZipUtils.unzip(packedService, tempFile);
		final File destFile = new File(tempFile, "ext/" + binaryName);
		if (!destFile.exists()) {
			throw new FileNotFoundException("deployment file/folder " + binaryName + " could not be found");
		}
		if (destFile.isDirectory()) {
			final File jarFile = File.createTempFile(
					destFile.getName(), ".jar");
			jarFile.delete();
			ZipUtils.zip(destFile, jarFile);
			return jarFile;
		}
		return destFile;
	}

	private ElasticStatelessProcessingUnitDeployment createElasticStatelessPUDeployment()
			throws ElasticDeploymentCreationException {
		final Service service = deploymentConfig.getService();
		final StatelessProcessingUnit puConfig;
		if (service.getStatelessProcessingUnit() != null) {
			puConfig = service.getStatelessProcessingUnit();
		} else {
			puConfig = service.getMirrorProcessingUnit();
		}
		final Sla statelessSla = puConfig.getSla();
		final int containerMemoryInMB = statelessSla.getMemoryCapacityPerContainer();
		prepareSla(statelessSla);
		// TODO: why is there no maxMemory?
		final File packedService = deploymentConfig.getPackedFile();
		final String binaryFileName = deploymentConfig.getService().getStatelessProcessingUnit().getBinaries();
		File binaryFile = null;
		try {
			binaryFile = getBinaryFromPackedRecipe(packedService, binaryFileName);
		} catch (final IOException e) {
			throw new ElasticDeploymentCreationException("Failed to open packaged service file: " + e.getMessage(), e);
		}

		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				binaryFile)
				.memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES);
		// Shared properties among all deployment types
		addSharedDeploymentParameters(deployment);
		addProcessingUnitContextProperties(deployment, puConfig.getContextProperties());

		setSpringProfilesActive(deployment, puConfig.getSpringProfilesActive());

		ManualCapacityScaleConfig scaleConfig;
		if (isLocalcloud()) {
			scaleConfig = new ManualCapacityScaleConfigurer()
					.memoryCapacity(containerMemoryInMB * service.getNumInstances(),
							MemoryUnit.MEGABYTES).create();
		} else {
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory();
			deployment.memoryCapacityPerContainer(
					(int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			// create manual standard scale config
			scaleConfig = ElasticScaleConfigFactory
					.createManualCapacityScaleConfig(containerMemoryInMB
							* service.getNumInstances(), 0,
							service.isLocationAware(), true);
		}
		deployment.scale(scaleConfig);
		return deployment;
	}

	private void setSpringProfilesActive(
			final ElasticDeploymentTopology deployment,
			final String profiles) {
		if (StringUtils.isNotBlank(profiles)) {
			deployment.commandLineArgument("-Dspring.profiles.active=" + profiles);
		}
	}

	private ElasticStatefulProcessingUnitDeployment createElasticStatefulProcessingUnit()
			throws ElasticDeploymentCreationException {
		final StatefulProcessingUnit puConfig = deploymentConfig.getService().getStatefulProcessingUnit();
		final Sla statefulSla = puConfig.getSla();
		prepareSla(statefulSla);

		final File packedService = deploymentConfig.getPackedFile();
		final String binaryFileName = deploymentConfig.getService().getStatefulProcessingUnit().getBinaries();
		File binaryFile = null;
		try {
			binaryFile = getBinaryFromPackedRecipe(packedService, binaryFileName);
		} catch (final IOException e) {
			throw new ElasticDeploymentCreationException("Failed to open packaged service file: " + e.getMessage(), e);
		}

		final ElasticStatefulProcessingUnitDeployment deployment =
				new ElasticStatefulProcessingUnitDeployment(binaryFile);
		// Shared properties among all deployment types
		addSharedDeploymentParameters(deployment);
		addProcessingUnitContextProperties(deployment, puConfig.getContextProperties());

		// TODO:The sla should also be available in a usm service.
		// these properties should be set if defined to overrride the properties set in the cloud groovy.
		final int containerMemoryInMB = statefulSla.getMemoryCapacityPerContainer();
		final int maxMemoryCapacityInMB = statefulSla.getMaxMemoryCapacity();
		deployment.memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryCapacityInMB, MemoryUnit.MEGABYTES)
				.highlyAvailable(statefulSla.getHighlyAvailable())
				.singleMachineDeployment();

		setSpringProfilesActive(deployment, puConfig.getSpringProfilesActive());
		ManualCapacityScaleConfig scaleConfig;
		final int memoryCapacity = statefulSla.getMemoryCapacity();
		if (isLocalcloud()) {
			// setting localcloud scale config
			scaleConfig = new ManualCapacityScaleConfigurer()
					.memoryCapacity(memoryCapacity, MemoryUnit.MEGABYTES).create();
		} else {
			// create manual standard scale config
			scaleConfig = ElasticScaleConfigFactory
					.createManualCapacityScaleConfig(memoryCapacity, 0,
							deploymentConfig.getService().isLocationAware(), true);
		}
		deployment.scale(scaleConfig);

		return deployment;
	}

	private ElasticStatelessProcessingUnitDeployment createElasticUSMDeployment()
			throws ElasticDeploymentCreationException {

		File packedFile = deploymentConfig.getPackedFile();
		if (packedFile.exists()) {
			packedFile = createExpectedFile(packedFile);
		}

		final ElasticStatelessProcessingUnitDeployment deployment =
				new ElasticStatelessProcessingUnitDeployment(packedFile);
		// Shared properties among all deployment types
		addSharedDeploymentParameters(deployment);
		deployment.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL, "true");

		final String disableSelHealingValue = Boolean.toString(!deploymentConfig.getInstallRequest().getSelfHealing());
		logger.info("Setting disable self healing value to: " + disableSelHealingValue);
		deployment.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_DISABLE_SELF_HEALING, disableSelHealingValue);
		deployment.name(this.deploymentConfig.getAbsolutePUName());

		final Service service = deploymentConfig.getService();
		final boolean scalingRulesDefined = service.getScalingRules() == null ? false : true;
		if (!isLocalcloud()) {
			// override provisioning config set in shared props since isolation
			// is not yet supported for every pu type.
			final boolean dedicated = IsolationUtils.isDedicated(service);
			final CloudifyMachineProvisioningConfig config = createCloudifyMachineProvisioningConfig();
			setIsolationConfig(deployment, dedicated, config);
			long cloudExternalProcessMemoryInMB;
			logger.info("setting deployment memory capacity for container according to isolation method");
			if (dedicated) {
				cloudExternalProcessMemoryInMB = calculateExternalProcessMemory();
			} else {
				cloudExternalProcessMemoryInMB = IsolationUtils.getInstanceMemoryMB(service);
				final long usmRequiredMemoryInMB = MemoryUnit.toMegaBytes(
						deploymentConfig.getCloud().getConfiguration().getComponents().getUsm().getMaxMemory());
				if (usmRequiredMemoryInMB > cloudExternalProcessMemoryInMB) {
					throw new IllegalStateException("the usm required memory "
							+ usmRequiredMemoryInMB
							+ " can not be more then the total memory defined for a service instance "
							+ cloudExternalProcessMemoryInMB);
				}
			}

			// this defines the total external memory for gsc and external process
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			if (scalingRulesDefined) {
				try {
					final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
							.createAutomaticCapacityScaleConfig(deploymentConfig.getAbsolutePUName(),
									service,
									(int) cloudExternalProcessMemoryInMB,
									service.isLocationAware(),
									dedicated);
					deployment.scale(scaleConfig);
				} catch (final Exception e) {
					throw new ElasticDeploymentCreationException("Failed creating scale config.", e);
				}
			} else {
				final int totalMemoryInMB = (int) cloudExternalProcessMemoryInMB
						* deploymentConfig.getService().getNumInstances();
				final double totalCpuCores = calculateTotalCpuCores(service);
				final ManualCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createManualCapacityScaleConfig(totalMemoryInMB,
								totalCpuCores, service.isLocationAware(), dedicated);
				deployment.scale(scaleConfig);
			}
		} else { // localcloud
			// this defines the total external memory for gsc and external process
			final int externalProcessMemoryInMB = 512;
			deployment.memoryCapacityPerContainer(externalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			if (scalingRulesDefined) {
				try {
					final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
							.createAutomaticCapacityScaleConfig(deploymentConfig.getAbsolutePUName(),
									service, externalProcessMemoryInMB, false, false);
					deployment.scale(scaleConfig);
				} catch (final Exception e) {
					throw new ElasticDeploymentCreationException("Failed creating scale config.", e);
				}
			} else {
				// the processing unit scales out whenever the specified memory capacity is breached
				deployment.scale(new ManualCapacityScaleConfigurer()
						.memoryCapacity(externalProcessMemoryInMB * deploymentConfig.getService().getNumInstances(),
								MemoryUnit.MEGABYTES).create());
			}
		}
		return deployment;
	}

	private File createExpectedFile(final File packedFile) {
		final String packedFileName = packedFile.getName();
		String fileType = "";
		final String[] split = packedFileName.split("\\.");
		if (split.length > 1) {
			fileType = "." + split[split.length - 1];
		}

		final String expectedName = this.deploymentConfig.getAbsolutePUName() + fileType;

		if (!packedFile.getName().equals(expectedName)) {
			final File renamedFile = new File(packedFile.getParentFile(), expectedName);
			if (renamedFile.exists()) {
				final boolean deleteResult = FileUtils.deleteQuietly(renamedFile);
				if (!deleteResult) {
					throw new IllegalStateException("Failed to delete file: " + renamedFile);
				}
			}
			if (!packedFile.renameTo(renamedFile)) {
				throw new IllegalStateException("Failed to rename " + packedFile.getAbsolutePath() + " to: "
						+ renamedFile.getAbsolutePath());
			}
			return renamedFile;

		}
		return packedFile;
	}

	private void prepareSla(final Sla sla) {

		if (isLocalcloud()) {
			return;
		}
		// Assuming one container per machine then container memory =
		// machine memory
		final int availableMemoryOnMachine = (int) calculateExternalProcessMemory();
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

			logger.info("Setting memoryCapacity for service " + deploymentConfig.getAbsolutePUName()
					+ " to minimum " + minMemoryInMB + "MB");
			sla.setMemoryCapacity(minMemoryInMB);
		}

		if (sla.getMaxMemoryCapacity() == null
				|| sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			logger.info("Setting maxMemoryCapacity for service " + deploymentConfig.getAbsolutePUName()
					+ " to memoryCapacity " + sla.getMemoryCapacity() + "MB");
			sla.setMaxMemoryCapacity(sla.getMemoryCapacity());
		}
	}

	private void setLocalcloudMachineProvisioningConfig(
			final ElasticDeploymentTopology deployment) {
		final int reservedMemoryPerMachineInMB = 256;
		final int reservedMemoryPerManagementMachineInMB = 256;
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		final DiscoveredMachineProvisioningConfig config = new DiscoveredMachineProvisioningConfigurer()
				.reservedMemoryCapacityPerMachine(reservedMemoryPerMachineInMB, MemoryUnit.MEGABYTES)
				.create();

		// localcloud is also the management machine
		config.setReservedMemoryCapacityPerManagementMachineInMB(reservedMemoryPerManagementMachineInMB);

		// TODO: This should be checked and removed. we no longer use zones.
		final String[] agentZones = new String[] { LOCALCLOUD_ZONE };
		config.setGridServiceAgentZones(agentZones);

		deployment.publicMachineProvisioning(config);
	}

	CloudifyMachineProvisioningConfig createCloudifyMachineProvisioningConfig() {

		final Cloud cloud = deploymentConfig.getCloud();
		final String serviceTemplateName = deploymentConfig.getTemplateName();
		final String managementMachineTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
		final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();

		final ComputeTemplate serviceTemplate = templates.get(serviceTemplateName);
		final ComputeTemplate managementTemplate = templates.get(managementMachineTemplateName);

		String storageTemplate = null;
		if (deploymentConfig.getService().getStorage() != null) {
			storageTemplate = deploymentConfig.getService().getStorage().getTemplate();
			logger.fine("Storage template " + storageTemplate + " is used with deployment");
		}
		logger.fine("Creating cloud machine provisioning config. Template remote directory is: "
				+ serviceTemplate.getRemoteDirectory());
		// TODO: is there any reason for passing the remote dir? this could be extracted from the cloud file.
		final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
				cloud, serviceTemplate, serviceTemplateName, managementTemplate.getRemoteDirectory(),
				storageTemplate);
		config.setAuthGroups(deploymentConfig.getAuthGroups());

		final String cloudOverrides = deploymentConfig.getCloudOverrides();
		if (cloudOverrides != null) {
			logger.fine("Recieved request for installation of "
					+ deploymentConfig.getAbsolutePUName() + " with cloud overrides parameters [ "
					+ cloudOverrides + "]");
			config.setCloudOverridesPerService(cloudOverrides);
		} else {
			logger.fine("No cloud overrides parameters were requested for the installation of "
					+ deploymentConfig.getAbsolutePUName());
		}
		// TODO: When is this used?
		addCloudConfigurationCostants(config);

		config.setLocator(deploymentConfig.getLocators());

		// adds the serialized network configuration to the config object
		addNetworkConfigurationToConfig(config);

		return config;
	}

	private void addNetworkConfigurationToConfig(final CloudifyMachineProvisioningConfig config) {
		if (this.deploymentConfig.getService().getNetwork() != null) {
			final ObjectMapper mapper = new ObjectMapper();
			try {
				final String serializedNetwork =
						mapper.writeValueAsString(this.deploymentConfig.getService().getNetwork());
				logger.info("Setting network string to: " + serializedNetwork);
				config.setNetworkAsString(serializedNetwork);
			} catch (final IOException e) {
				throw new IllegalArgumentException(
						"Failed to serialize service network description into a JSON string", e);
			}

		}
	}

	private void addCloudConfigurationCostants(
			final CloudifyMachineProvisioningConfig config) {
		final byte[] cloudConfig = deploymentConfig.getCloudConfig();
		if (cloudConfig != null) {
			config.setServiceCloudConfiguration(cloudConfig);
		}
	}

	private boolean isLocalcloud() {
		if (deploymentConfig.getCloud() == null) {
			return true;
		}
		return false;
	}

	// This method is currently used only for USM type services.
	private void setIsolationConfig(
			final ElasticDeploymentTopology deployment,
			final boolean dedicated, final CloudifyMachineProvisioningConfig config) {
		final Service service = deploymentConfig.getService();
		if (IsolationUtils.isUseManagement(service)) {
			config.setDedicatedManagementMachines(false);
		} else {
			config.setDedicatedManagementMachines(true);
		}
		if (dedicated) {
			// service deployment will have a dedicated agent per instance
			deployment.dedicatedMachineProvisioning(config);
		} else {
			// check what mode of isolation we should use
			if (IsolationUtils.isGlobal(service)) {
				logger.info("global mode is on. will use public machine provisioning for "
						+ deploymentConfig.getAbsolutePUName() + " deployment.");
				logger.info("isolationSLA = " + service.getIsolationSLA());
				// service instances can be deployed across all agents
				deployment.publicMachineProvisioning(config);

			} else if (IsolationUtils.isAppShared(service)) {
				final String applicationName = deploymentConfig.getApplicationName();
				logger.info("app shared mode is on. will use shared machine provisioning for "
						+ deploymentConfig.getAbsolutePUName() + " deployment. isolation id = " + applicationName);
				// service instances can be deployed across all agents with the correct isolation id
				deployment.sharedMachineProvisioning(applicationName, config);
			} else if (IsolationUtils.isTenantShared(service)) {
				final String authGroups = deploymentConfig.getAuthGroups();
				if (authGroups == null) {
					throw new IllegalStateException("authGroups cannot be null when using tenant shared isolation");
				}
				logger.info("tenant shared mode is on. will use shared machine provisioning for "
						+ deploymentConfig.getAbsolutePUName() + " deployment. isolation id = " + authGroups);
				// service instances can be deployed across all agents with the correct isolation id
				deployment.sharedMachineProvisioning(authGroups, config);
			}
			if (IsolationUtils.isGlobal(service) && IsolationUtils.isUseManagement(service)) {
				final String managementMachineTemplateName = deploymentConfig.getCloud().
						getConfiguration().getManagementMachineTemplate();
				final ComputeDetails compute = service.getCompute();
				if (compute != null) {
					if (compute.getTemplate() != null && !compute.getTemplate().isEmpty()) {
						if (!compute.getTemplate().equals(managementMachineTemplateName)) {
							// this is just a clarification log.
							// the service wont be installed on a management machine(even if there is enough memory)
							// because the management machine template does not match the desired template
							logger.warning("Installation of service " + service.getName() + " on a management machine "
									+ "will not be attempted since the specified template(" + compute.getTemplate()
									+ ")"
									+ " is different than the management machine template("
									+ managementMachineTemplateName + ")");
						}
					}
				} else {
					// normal default is to the first template. but when specified useManagement
					// do a silent default to the management machine template.
					deploymentConfig.setTemplateName(managementMachineTemplateName);
				}
			}
		}
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
		final double instanceCpuCores = IsolationUtils.getInstanceCpuCores(service);
		final int numberOfInstances = service.getNumInstances();
		return numberOfInstances * instanceCpuCores;
	}

	// calculate the external process memory according to the
	// properties set in the template and the reserved capacity for machine
	private long calculateExternalProcessMemory() {
		final String templateName = deploymentConfig.getTemplateName();
		final Cloud cloud = deploymentConfig.getCloud();
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(templateName);
		// TODO remove hardcoded number
		logger.fine("Calculating external proc mem for template: " + template);
		final int machineMemoryMB = template.getMachineMemoryMB();
		final int reservedMemoryCapacityPerMachineInMB = cloud.getProvider()
				.getReservedMemoryCapacityPerMachineInMB();
		final int safteyMargin = 100; // get rid of this constant. see
		// CLOUDIFY-297
		long cloudExternalProcessMemoryInMB = machineMemoryMB
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

		// USM can not require more memory then the maximum total memory available.
		final long usmRequiredMemoryInMB = MemoryUnit.toMegaBytes(
				cloud.getConfiguration().getComponents().getUsm().getMaxMemory());
		cloudExternalProcessMemoryInMB = Math.max(cloudExternalProcessMemoryInMB, usmRequiredMemoryInMB);
		return cloudExternalProcessMemoryInMB;
	}

	/**
	 * Create Properties object with settings from the service object, if found on the given service. The supported
	 * settings are: com.gs.application.dependsOn com.gs.service.type com.gs.service.icon
	 * com.gs.service.network.protocolDescription
	 * 
	 * The service object the read the settings from
	 * 
	 * @return Properties object populated with the above properties, if found on the given service.
	 */
	private Properties createServiceContextProperties() {

		final Properties contextProperties = new Properties();

		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
				deploymentConfig.getApplicationName());
		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS,
				deploymentConfig.getAuthGroups());
		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEPLOYMENT_ID,
				deploymentConfig.getDeploymentId());
		if (deploymentConfig.getTemplateName() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE,
					deploymentConfig.getTemplateName());
		}
		final Service service = deploymentConfig.getService();

		final List<String> dependsOn = service.getDependsOn();
		if (dependsOn.isEmpty()) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
		} else {
			final List<String> absoluteServiceNames = new ArrayList<String>();
			for (final String name : dependsOn) {
				absoluteServiceNames.add(ServiceUtils.getAbsolutePUName(
						deploymentConfig.getApplicationName(), name.trim()));
			}
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON,
					Arrays.toString(absoluteServiceNames.toArray()));

		}

		if (service.getType() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE,
					service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER
							+ service.getIcon());
		}
		if (service.getNetwork() != null) {
			if (service.getNetwork().getProtocolDescription() != null) {
				contextProperties
						.setProperty(
								CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
								service.getNetwork().getProtocolDescription());
			}
		}

		final String cloudName = getCloudName();

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME,
				cloudName);

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));

		final InstallServiceRequest installRequest = deploymentConfig.getInstallRequest();
		if (installRequest.isDebugAll()) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_ALL, Boolean.TRUE.toString());
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE,
					installRequest.getDebugMode());
		} else if (installRequest.getDebugEvents() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_EVENTS,
					installRequest.getDebugEvents());
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE,
					installRequest.getDebugMode());
		}

		return contextProperties;
	}

	private String getCloudName() {
		if (deploymentConfig.getCloud() != null) {
			return deploymentConfig.getCloud().getName();
		} else {
			return CloudifyConstants.LOCAL_CLOUD_NAME;
		}
	}

	// adds all shared properties among all deployment types
	private void addSharedDeploymentParameters(
			final ElasticDeploymentTopology deployment) {
		deployment.name(deploymentConfig.getAbsolutePUName());
		// add context properties
		final Properties contextProperties = createServiceContextProperties();
		setContextProperties(deployment, contextProperties);

		if (!isLocalcloud()) {
			logger.fine("setting lrmi bind ports and container memory context properties");
			deployment.addCommandLineArgument("-D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "="
					+ deploymentConfig.getCloud().getConfiguration().getComponents().getUsm().getPortRange())
					.addCommandLineArgument("-Xmx" + deploymentConfig.getCloud().getConfiguration().getComponents()
							.getUsm().getMaxMemory())
					.addCommandLineArgument("-Xms" + deploymentConfig.getCloud().getConfiguration().getComponents()
							.getUsm().getMinMemory());

			final CloudifyMachineProvisioningConfig config = createCloudifyMachineProvisioningConfig();
			// TODO: uncomment this when isolation is defined for pu types.
			// isolation should work out of the box for every pu type.
			// setIsolationConfig(deployment, dedicated, config);
			deployment.dedicatedMachineProvisioning(config);

		} else { // localcloud
			setLocalcloudMachineProvisioningConfig(deployment);
		}
	}
}
