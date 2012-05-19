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
package org.cloudifysource.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.cloudifysource.dsl.statistics.PerInstanceStatisticsDetails;
import org.cloudifysource.dsl.statistics.ServiceStatisticsDetails;
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

	private ServiceLifecycle lifecycle;
	private UserInterface userInterface;

	private List<PluginDescriptor> plugins;

	private List<String> dependsOn = new LinkedList<String>();

	private ServiceNetwork network;

	private int numInstances = 1;

	private int minAllowedInstances = 1;

	private int maxAllowedInstances = 1;

	private long maxJarSize = DEFAULT_MAX_JAR_SIZE;

	private Map<String, Object> customCommands = new HashMap<String, Object>();

	private String type;

	private StatelessProcessingUnit statelessProcessingUnit;

	private StatefulProcessingUnit statefulProcessingUnit;

	private DataGrid datagrid;

	private Memcached memcachedProcessingUnit;

	private MirrorProcessingUnit mirrorProcessingUnit;

	private Map<String, String> customProperties = new HashMap<String, String>();

	private ComputeDetails compute;

	private LinkedList<String> extendedServicesPaths = new LinkedList<String>();

	private boolean elastic = false;

	private String url = null;

	private List<ScalingRuleDetails> scalingRules;

	private long scaleOutCooldownInSeconds = 0;
	private long scaleInCooldownInSeconds = 0;

	private List<ServiceStatisticsDetails> serviceStatistics;
	private List<PerInstanceStatisticsDetails> perInstanceStatistics;

	private long samplingPeriodInSeconds = DEFAULT_SAMPLING_PERIOD_SECONDS;

	public long getSamplingPeriodInSeconds() {
		return samplingPeriodInSeconds;
	}

	/**
	 * @param samplingPeriodInSeconds The time (in seconds) between two consecutive metric samples. This figure should
	 *        be set when using scale rules
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
	 * @param statelessProcessingUnit .
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
	 * @param mirrorProcessingUnit .
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
	 * @param statefulProcessingUnit .
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
	 * @param dataGrid .
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
	 * @param memcached .
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

	public Map<String, Object> getCustomCommands() {
		return customCommands;
	}

	public void setCustomCommands(final Map<String, Object> customCommands) {
		this.customCommands = customCommands;
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

	public long getScaleOutCooldownInSeconds() {
		return scaleOutCooldownInSeconds;
	}

	/**
	 * 
	 * @param scaleOutCooldownInSeconds - The time (in seconds) that scaling rules are disabled after scale out
	 *        (instances added)
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
	 * @param scaleInCooldownInSeconds - The time (in seconds) that scaling rules are disabled after scale in (instances
	 *        removed)
	 * @see #setScaleCooldownInSeconds(long)
	 * @see #setScaleOutCooldownInSeconds(long)
	 */
	public void setScaleInCooldownInSeconds(final long scaleInCooldownInSeconds) {
		this.scaleInCooldownInSeconds = scaleInCooldownInSeconds;
	}

	/**
	 * 
	 * @param scaleCooldownInSeconds - The time (in seconds) that scaling rules are disabled after scale in (instances
	 *        removed) or scale out (instances added)
	 * 
	 *        This has the same effect as calling {@link #setScaleInCooldownInSeconds(long)} and
	 *        {@link #setScaleOutCooldownInSeconds(long)} separately.
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
	void validateDefaultValues()
			throws DSLValidationException {
		if (this.numInstances > this.maxAllowedInstances) {
			throw new DSLValidationException("The requested number of instances ("
					+ this.numInstances + ") exceeds the maximum number of instances allowed"
					+ " (" + this.maxAllowedInstances + ") for service " + this.name + ".");
		}
	}

}
