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
package org.cloudifysource.dsl;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.entry.ExecutableEntriesMap;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceTierType;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.cloudifysource.dsl.statistics.PerInstanceStatisticsDetails;
import org.cloudifysource.dsl.statistics.ServiceStatisticsDetails;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.Unit;
import org.openspaces.ui.UserInterface;

/****************************
 * The POJO for a service running in Cloudify. All of the details required to run a specific service are available here.
 * This is the main configuration object used by the USM to install, run, monitor and stop a process.
 *
 * @author barakme
 * @since 1.0.0
 *
 */
@CloudifyDSLEntity(name = "service", clazz = Service.class, allowInternalNode = true, allowRootNode = true,
		parent = "application")
public class Service {

	private static final int DEFAULT_MAX_JAR_SIZE = 150 * 1024 * 1024; // 150 MB
	private static final long DEFAULT_SAMPLING_PERIOD_SECONDS = 60;

	/******
	 * The service Name.
	 */
	private String name;
	private String icon;

	private IsolationSLA isolationSLA;

	private ServiceLifecycle lifecycle;
	private UserInterface userInterface;

	private List<PluginDescriptor> plugins;

	private List<String> dependsOn = new LinkedList<String>();

	private ServiceNetwork network;

	private int numInstances = 1;

	private int minAllowedInstances = 1;

	private int maxAllowedInstances = 1;

	private int minAllowedInstancesPerLocation = 1;

	private int maxAllowedInstancesPerLocation = 1;

	private long maxJarSize = DEFAULT_MAX_JAR_SIZE;

	private ExecutableEntriesMap customCommands = new ExecutableEntriesMap();

	private String type = ServiceTierType.UNDEFINED.toString();

	private StatelessProcessingUnit statelessProcessingUnit;

	private StatefulProcessingUnit statefulProcessingUnit;

	private DataGrid datagrid;

	private Memcached memcachedProcessingUnit;

	private MirrorProcessingUnit mirrorProcessingUnit;

	private Map<String, String> customProperties = new HashMap<String, String>();

	private ComputeDetails compute;
	
	private StorageDetails storage = new StorageDetails();

	private LinkedList<String> extendedServicesPaths = new LinkedList<String>();

	private boolean elastic = false;

	private String url = null;

	private List<ScalingRuleDetails> scalingRules;

	private long scaleOutCooldownInSeconds = 0;
	private long scaleInCooldownInSeconds = 0;

	private List<ServiceStatisticsDetails> serviceStatistics;
	private List<PerInstanceStatisticsDetails> perInstanceStatistics;

	private long samplingPeriodInSeconds = DEFAULT_SAMPLING_PERIOD_SECONDS;

	private boolean locationAware = false;

	public IsolationSLA getIsolationSLA() {
		return isolationSLA;
	}

	public void setIsolationSLA(final IsolationSLA isolationSLA) {
		this.isolationSLA = isolationSLA;
	}

	public long getSamplingPeriodInSeconds() {
		return samplingPeriodInSeconds;
	}

	/**
	 * @param samplingPeriodInSeconds
	 *            The time (in seconds) between two consecutive metric samples. This figure should be set when using
	 *            scale rules
	 */
	public void setSamplingPeriodInSeconds(final long samplingPeriodInSeconds) {
		this.samplingPeriodInSeconds = samplingPeriodInSeconds;
	}

	public boolean isElastic() {
		return elastic;
	}

	public void setElastic(final boolean elastic) {
		this.elastic = elastic;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Service [name=" + name + ", icon=" + icon + "]";
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(final String icon) {
		this.icon = icon;
	}

	public ServiceLifecycle getLifecycle() {
		return lifecycle;
	}

	public void setLifecycle(final ServiceLifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}

	public ServiceNetwork getNetwork() {
		return this.network;
	}

	public void setNetwork(final ServiceNetwork network) {
		this.network = network;
	}

	public UserInterface getUserInterface() {
		return userInterface;
	}

	public void setUserInterface(final UserInterface userInterface) {
		this.userInterface = userInterface;
	}

	public void setPlugins(final List<PluginDescriptor> plugins) {
		this.plugins = plugins;
	}

	public void setServiceStatistics(final List<ServiceStatisticsDetails> calculatedStatistics) {
		this.serviceStatistics = calculatedStatistics;
	}

	public List<PerInstanceStatisticsDetails> getPerInstanceStatistics() {
		return this.perInstanceStatistics;
	}

	public void setPerInstanceStatistics(final List<PerInstanceStatisticsDetails> perInstanceStatistics) {
		this.perInstanceStatistics = perInstanceStatistics;
	}

	public List<ServiceStatisticsDetails> getServiceStatistics() {
		return this.serviceStatistics;
	}

	public List<PluginDescriptor> getPlugins() {
		return plugins;
	}

	public int getNumInstances() {
		return numInstances;
	}

	public void setNumInstances(final int numInstances) {
		this.numInstances = numInstances;
	}

	public long getMaxJarSize() {
		return maxJarSize;
	}

	public void setMaxJarSize(final long maxJarSize) {
		this.maxJarSize = maxJarSize;
	}

	public Map<String, String> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(final Map<String, String> customProperties) {
		this.customProperties = customProperties;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	/******
	 * .
	 *
	 * @param statelessProcessingUnit
	 *            .
	 */
	public void setStatelessProcessingUnit(final StatelessProcessingUnit statelessProcessingUnit) {
		if (this.statelessProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
		this.statelessProcessingUnit = statelessProcessingUnit;
	}

	/*************
	 * .
	 *
	 * @param mirrorProcessingUnit
	 *            .
	 */
	public void setMirrorProcessingUnit(final MirrorProcessingUnit mirrorProcessingUnit) {
		if (this.mirrorProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
		this.mirrorProcessingUnit = mirrorProcessingUnit;
	}

	/*******
	 * .
	 *
	 * @param statefulProcessingUnit
	 *            .
	 */
	public void setStatefulProcessingUnit(final StatefulProcessingUnit statefulProcessingUnit) {
		if (this.statefulProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
		this.statefulProcessingUnit = statefulProcessingUnit;
	}

	/**************
	 * .
	 *
	 * @param dataGrid
	 *            .
	 */
	public void setDataGrid(final DataGrid dataGrid) {
		if (this.datagrid != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
		this.datagrid = dataGrid;
	}

	/**********
	 * .
	 *
	 * @param memcached
	 *            .
	 */
	public void setMemcached(final Memcached memcached) {
		if (this.memcachedProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then one ProcessingUnit type");
		}

		this.memcachedProcessingUnit = memcached;
	}

	public Memcached getMemcached() {
		return this.memcachedProcessingUnit;
	}

	public StatelessProcessingUnit getStatelessProcessingUnit() {
		return this.statelessProcessingUnit;
	}

	public MirrorProcessingUnit getMirrorProcessingUnit() {
		return this.mirrorProcessingUnit;
	}

	public StatefulProcessingUnit getStatefulProcessingUnit() {
		return this.statefulProcessingUnit;
	}

	public DataGrid getDataGrid() {
		return this.datagrid;
	}

	public List<String> getDependsOn() {
		return dependsOn;
	}

	public void setDependsOn(final List<String> dependsOn) {
		this.dependsOn = dependsOn;
	}

	public ComputeDetails getCompute() {
		return compute;
	}

	public void setCompute(final ComputeDetails compute) {
		this.compute = compute;
	}

	public void setExtendedServicesPaths(final LinkedList<String> extendedServicesPaths) {
		this.extendedServicesPaths = extendedServicesPaths;
	}

	public LinkedList<String> getExtendedServicesPaths() {
		return extendedServicesPaths;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public List<ScalingRuleDetails> getScalingRules() {
		return scalingRules;
	}

	public void setScalingRules(final List<ScalingRuleDetails> scalingRules) {
		this.scalingRules = scalingRules;
	}

	public int getMinAllowedInstances() {
		return minAllowedInstances;
	}

	public void setMinAllowedInstances(final int minAllowedInstances) {
		this.minAllowedInstances = minAllowedInstances;
	}

	public int getMaxAllowedInstances() {
		return maxAllowedInstances;
	}

	public void setMaxAllowedInstances(final int maxAllowedInstances) {
		this.maxAllowedInstances = maxAllowedInstances;
	}

	public int getMinAllowedInstancesPerLocation() {
		return minAllowedInstancesPerLocation;
	}

	public void setMinAllowedInstancesPerLocation(final int minAllowedInstancesPerLocation) {
		this.minAllowedInstancesPerLocation = minAllowedInstancesPerLocation;
	}

	public int getMaxAllowedInstancesPerLocation() {
		return maxAllowedInstancesPerLocation;
	}

	public void setMaxAllowedInstancesPerLocation(final int maxAllowedInstancesPerLocation) {
		this.maxAllowedInstancesPerLocation = maxAllowedInstancesPerLocation;
	}

	public long getScaleOutCooldownInSeconds() {
		return scaleOutCooldownInSeconds;
	}

	/**
	 *
	 * @param scaleOutCooldownInSeconds
	 *            - The time (in seconds) that scaling rules are disabled after scale out (instances added)
	 *
	 * @see #setScaleOutCooldownInSeconds(long)
	 * @see #setScaleCooldownInSeconds(long)
	 */
	public void setScaleOutCooldownInSeconds(final long scaleOutCooldownInSeconds) {
		this.scaleOutCooldownInSeconds = scaleOutCooldownInSeconds;
	}

	public long getScaleInCooldownInSeconds() {
		return scaleInCooldownInSeconds;
	}

	/**
	 *
	 * @param scaleInCooldownInSeconds
	 *            - The time (in seconds) that scaling rules are disabled after scale in (instances removed)
	 * @see #setScaleCooldownInSeconds(long)
	 * @see #setScaleOutCooldownInSeconds(long)
	 */
	public void setScaleInCooldownInSeconds(final long scaleInCooldownInSeconds) {
		this.scaleInCooldownInSeconds = scaleInCooldownInSeconds;
	}

	/**
	 *
	 * @param scaleCooldownInSeconds
	 *            - The time (in seconds) that scaling rules are disabled after scale in (instances removed) or scale
	 *            out (instances added)
	 *
	 *            This has the same effect as calling {@link #setScaleInCooldownInSeconds(long)} and
	 *            {@link #setScaleOutCooldownInSeconds(long)} separately.
	 *
	 * @see #setScaleInCooldownInSeconds(long)
	 * @see #setScaleOutCooldownInSeconds(long)
	 */
	public void setScaleCooldownInSeconds(final long scaleCooldownInSeconds) {
		this.scaleOutCooldownInSeconds = scaleCooldownInSeconds;
		this.scaleInCooldownInSeconds = scaleCooldownInSeconds;
	}

	/**
	 *
	 * @return the time in seconds that scaling rules are disabled after scale in or scale out. In case the scale in and
	 *         scale out values are different it returns the bigger value.
	 */
	public long getScaleCooldownInSeconds() {
		return Math.max(this.scaleOutCooldownInSeconds, this.scaleInCooldownInSeconds);
	}

	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		validateInstanceNumber();
		validateServiceType();
	}

	private void validateServiceType() throws DSLValidationException {
		boolean typeExists = false;
		String[] enumAsString = new String[ServiceTierType.values().length];
		int counter = 0;
		for (ServiceTierType tierType : ServiceTierType.values()) {
			enumAsString[counter] = tierType.toString();
			counter++;
			if (tierType.toString().equalsIgnoreCase(this.type)) {
				typeExists = true;
				break;
			}
		}
		if (!typeExists) {
			throw new DSLValidationException("The service type '" + this.type + "' is undefined."
					+ "The known service types include " + Arrays.toString(enumAsString));
		}
	}

	private void validateInstanceNumber() throws DSLValidationException {
		if (this.numInstances > this.maxAllowedInstances) {
			throw new DSLValidationException("The requested number of instances ("
					+ this.numInstances + ") exceeds the maximum number of instances allowed"
					+ " (" + this.maxAllowedInstances + ") for service " + this.name + ".");
		}
	}

	/**
	 * Validate the icon property (if set) points to an existing file.
	 *
	 * @param validationContext
	 *            The DSLValidationContext object
	 * @throws DSLValidationException
	 *             Indicates the icon could not be found
	 */
	@DSLValidation
	void validateIcon(final DSLValidationContext validationContext) throws DSLValidationException {
		boolean isServiceFile = false;
		String serviceSuffix = DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX.trim();
		String filePath = validationContext.getFilePath().trim();

		// execute this validation only if an icon was set and this is a Service's groovy file (not an Application's)
		if (filePath.endsWith(serviceSuffix)) {
			isServiceFile = true;
		}

		if (icon != null && isServiceFile) {
			File dslFile = new File(filePath);
			File iconFile = new File(dslFile.getParent(), icon);

			if (!iconFile.isFile()) {

				// check the icon at the extended path location, required for service that extend another Service file

				File dslDirectory = dslFile.getParentFile();
				if (getExtendedServicesPaths() != null) {
					for (final String extendedPath : getExtendedServicesPaths()) {
						if (isAbsolutePath(extendedPath)) {
							dslDirectory = new File(extendedPath);
						} else {
							// climb up the extend hierarchy to look for the icno
							dslDirectory = new File(dslDirectory.getAbsolutePath() + "/" + extendedPath);
						}
						iconFile = new File(dslDirectory.getAbsolutePath(), icon);
						if (iconFile.isFile()) {
							break;
						}
					}
				}

				if (!iconFile.isFile()) {
					throw new DSLValidationException("The icon file \"" + iconFile.getAbsolutePath()
							+ "\" does not exist.");
				}

			}
		}

	}

	private boolean isAbsolutePath(final String extendedPath) {
		return new File(extendedPath).isAbsolute();
	}

	@DSLValidation
	void validateCustomProperties(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.customProperties.containsKey(CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT)) {
			try {
				Long.parseLong(this.customProperties
						.get(CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT));

			} catch (final NumberFormatException e) {
				throw new DSLValidationException("The "
						+ CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT
						+ " property must be a long value", e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@DSLValidation
	void validateUserInterfaceObjectIsWellDefined(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.userInterface != null) {
			// Validate metric list
			List<MetricGroup> metricGroups = this.userInterface.getMetricGroups();
			for (MetricGroup metricGroup : metricGroups) {
				for (Object metric : metricGroup.getMetrics()) {
					if (metric instanceof List<?>) {
						if (!(((List) metric).get(0) instanceof String)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric name should be of type 'String'");
						}
						if (!(((List) metric).get(1) instanceof Unit)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric axisYUnit should be of type org.openspaces.ui.Unit");
						}
						if (!(((List) metric).size() == 2)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric should be defined as String or as a list [String, Unit]");
						}
					} else {
						if (!(metric instanceof String)) {
							throw new DSLValidationException("the defined metric " + metric.toString() + " is invalid."
									+
									" metric name should be of type 'String'");
						}
					}
				}
			}
		}
	}

	/**
	 * Validates that the name property exists and is not empty or invalid.
	 *
	 * @param validationContext
	 * @throws DSLValidationException
	 */
	@DSLValidation
	void validateName(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (StringUtils.isBlank(name)) {
			throw new DSLValidationException("Service.validateName: The service's name "
					+ (name == null ? "is missing" : "is empty"));
		}

		DSLUtils.validateRecipeName(name);
	}

	public ExecutableEntriesMap getCustomCommands() {
		return customCommands;
	}

	public void setCustomCommands(final ExecutableEntriesMap customCommands) {
		this.customCommands = customCommands;
	}

	public boolean isLocationAware() {
		return locationAware;
	}

	public void setLocationAware(final boolean locationAware) {
		this.locationAware = locationAware;
	}

	public StorageDetails getStorage() {
		return storage;
	}

	public void setStorage(final StorageDetails storage) {
		this.storage = storage;
	}
}