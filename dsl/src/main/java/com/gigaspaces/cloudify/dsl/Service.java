package com.gigaspaces.cloudify.dsl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openspaces.ui.UserInterface;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "service", clazz = Service.class, allowInternalNode = true, allowRootNode = true, parent = "application")
public class Service implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String icon;
	private String errorLoggerName;
	private String outputLoggerName;

	private String imageTemplate;
	private String defaultScalingUnit;
	private String pidFile;
	private boolean supportsScaling;

	private ServiceLifecycle lifecycle;
	private UserInterface userInterface;

	private List<PluginDescriptor> plugins;

	private List<String> dependsOn = new LinkedList<String>();

	private ServiceNetwork network;

	private int numInstances = 1;
	private long maxJarSize = 150 * 1024 * 1024; // in bytes
	private boolean keepFile = false;

	
	private Map<String, Object> customCommands = new HashMap<String, Object>();

	private String type;

	private ServiceProcessingUnit serviceProcessingUnit;
	
	private Map<String, String> customProperties = new HashMap<String, String>();
	
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Service [name=" + name + "]";
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

	public String getImageTemplate() {
		return imageTemplate;
	}

	public void setImageTemplate(final String imageTemplate) {
		this.imageTemplate = imageTemplate;
	}

	public String getDefaultScalingUnit() {
		return defaultScalingUnit;
	}

	public void setDefaultScalingUnit(final String defaultScalingUnit) {
		this.defaultScalingUnit = defaultScalingUnit;
	}

	public boolean isSupportsScaling() {
		return supportsScaling;
	}

	public void setSupportsScaling(final boolean supportsScaling) {
		this.supportsScaling = supportsScaling;
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

	public boolean isKeepFile() {
		return keepFile;
	}

	public void setKeepFile(final boolean keepFile) {
		this.keepFile = keepFile;
	}

	public Map<String, String> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(final Map<String, String> customProperties) {
		this.customProperties = customProperties;
	}

	public String getPidFile() {
		return pidFile;
	}

	public void setPidFile(final String pidFile) {
		this.pidFile = pidFile;
	}

	public String getErrorLoggerName() {
		return this.errorLoggerName;
	}

	public String getOutputLoggerName() {
		return outputLoggerName;
	}

	public void setOutputLoggerName(final String outputLoggerName) {
		this.outputLoggerName = outputLoggerName;
	}

	public void setErrorLoggerName(final String errorLoggerName) {
		this.errorLoggerName = errorLoggerName;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setStatelessProcessingUnit(
			StatelessProcessingUnit statelessProcessingUnit) {
		if (this.serviceProcessingUnit == null) {
			this.serviceProcessingUnit = statelessProcessingUnit;
		} else {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit state");
		}
	}

	public void setMirrorProcessingUnit(
			MirrorProcessingUnit mirrorProcessingUnit) {
		if (this.serviceProcessingUnit == null) {
			this.serviceProcessingUnit = mirrorProcessingUnit;
		} else {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit state");
		}
	}

	public void setStatefulProcessingUnit(
			StatefulProcessingUnit statefulProcessingUnit) {
		if (this.serviceProcessingUnit == null) {
			this.serviceProcessingUnit = statefulProcessingUnit;
		} else {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit state");
		}
	}

	public void setDataGrid(DataGrid dataGrid) {
		if (this.serviceProcessingUnit == null) {
			this.serviceProcessingUnit = dataGrid;
		} else {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit state");
		}
	}

	public void setMemcached(ServiceProcessingUnit memcached) {
		if (this.serviceProcessingUnit == null) {
			this.serviceProcessingUnit = memcached;
		} else {
			throw new IllegalStateException(
					"DSL File contains more then one ProcessingUnit state");
		}
	}

	public Memcached getMemcached() {
		Memcached service = null;
		if (this.serviceProcessingUnit instanceof Memcached) {
			service = (Memcached) serviceProcessingUnit;
		}
		return service;
	}

	public StatelessProcessingUnit getStatelessProcessingUnit() {
		StatelessProcessingUnit service = null;
		if (this.serviceProcessingUnit instanceof StatelessProcessingUnit) {
			service = (StatelessProcessingUnit) serviceProcessingUnit;
		}
		return service;
	}

	public StatelessProcessingUnit getMirrorProcessingUnit() {
		StatelessProcessingUnit service = null;
		if (this.serviceProcessingUnit instanceof MirrorProcessingUnit) {
			service = (MirrorProcessingUnit) serviceProcessingUnit;
		}
		return service;
	}

	public StatefulProcessingUnit getStatefulProcessingUnit() {
		StatefulProcessingUnit service = null;
		if (this.serviceProcessingUnit instanceof StatefulProcessingUnit) {
			service = (StatefulProcessingUnit) serviceProcessingUnit;
		}
		return service;
	}

	public DataGrid getDataGrid() {
		DataGrid service = null;
		if (this.serviceProcessingUnit instanceof DataGrid) {
			service = (DataGrid) serviceProcessingUnit;
		}
		return service;
	}

	public Map<String, Object> getCustomCommands() {
		return customCommands;
	}

	public void setCustomCommands(Map<String, Object> customCommands) {
		this.customCommands = customCommands;
	}

	public List<String> getDependsOn() {
		return dependsOn;
	}

	public void setDependsOn(List<String> dependsOn) {
		this.dependsOn = dependsOn;
	}
}
