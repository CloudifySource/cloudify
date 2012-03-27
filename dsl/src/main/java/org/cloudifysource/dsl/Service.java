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

import org.cloudifysource.dsl.autoscaling.AutoScalingDetails;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
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
	
	private int minNumInstances = 1;
	
	private int maxNumInstances = 1;
	
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
	
	private AutoScalingDetails autoScaling;
	
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
		if (this.statelessProcessingUnit == null) {
			this.statelessProcessingUnit = statelessProcessingUnit;
		} else if (this.statelessProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
	}

	/*************
	 * .
	 * 
	 * @param mirrorProcessingUnit
	 *            .
	 */
	public void setMirrorProcessingUnit(final MirrorProcessingUnit mirrorProcessingUnit) {
		if (this.mirrorProcessingUnit == null) {
			this.mirrorProcessingUnit = mirrorProcessingUnit;
		} else if (this.mirrorProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
	}

	/*******
	 * .
	 * 
	 * @param statefulProcessingUnit
	 *            .
	 */
	public void setStatefulProcessingUnit(final StatefulProcessingUnit statefulProcessingUnit) {
		if (this.statefulProcessingUnit == null) {
			this.statefulProcessingUnit = statefulProcessingUnit;
		} else if (this.statefulProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
	}

	/**************
	 * .
	 * 
	 * @param dataGrid
	 *            .
	 */
	public void setDataGrid(final DataGrid dataGrid) {
		if (this.datagrid == null) {
			this.datagrid = dataGrid;
		} else if (this.datagrid != null) {
			throw new IllegalStateException("DSL File contains more then 1 ProcessingUnit type");
		}
	}

	/**********
	 * .
	 * 
	 * @param memcached .
	 */
	public void setMemcached(final Memcached memcached) {
		if (this.memcachedProcessingUnit == null) {
			this.memcachedProcessingUnit = memcached;
		} else if (this.memcachedProcessingUnit != null) {
			throw new IllegalStateException("DSL File contains more then one ProcessingUnit type");
		}
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

	public AutoScalingDetails getAutoScaling() {
		return autoScaling;
	}

	public void setAutoScaling(final AutoScalingDetails autoScaling) {
		this.autoScaling = autoScaling;
	}

	public int getMinNumInstances() {
		return minNumInstances;
	}

	public void setMinNumInstances(int minNumInstances) {
		this.minNumInstances = minNumInstances;
	}

	public int getMaxNumInstances() {
		return maxNumInstances;
	}

	public void setMaxNumInstances(int maxNumInstances) {
		this.maxNumInstances = maxNumInstances;
	}

}
