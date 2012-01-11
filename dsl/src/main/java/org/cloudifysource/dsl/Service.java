package org.cloudifysource.dsl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.openspaces.ui.UserInterface;


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

	private StatelessProcessingUnit statelessProcessingUnit;
	
	private StatefulProcessingUnit statefulProcessingUnit;
	
	private DataGrid datagrid;
	
	private Memcached memcachedProcessingUnit;
	
	private MirrorProcessingUnit mirrorProcessingUnit;
	
	private Map<String, String> customProperties = new HashMap<String, String>();

	private ComputeDetails compute;
	
	private LinkedList<String> extendedServicesPaths = new LinkedList<String>();
	
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
		if (this.statelessProcessingUnit == null) {
			this.statelessProcessingUnit = statelessProcessingUnit;
		} else if (this.statelessProcessingUnit != null) {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit type");
		}
	}

	public void setMirrorProcessingUnit(
			MirrorProcessingUnit mirrorProcessingUnit) {
		if (this.mirrorProcessingUnit == null) {
			this.mirrorProcessingUnit = mirrorProcessingUnit;
		} else if (this.mirrorProcessingUnit != null) {
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit type");
		}
	}

	public void setStatefulProcessingUnit(
			StatefulProcessingUnit statefulProcessingUnit) {
		if (this.statefulProcessingUnit == null) {
			this.statefulProcessingUnit = statefulProcessingUnit;
		} else if (this.statefulProcessingUnit != null){
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit type");
		}
	}

	public void setDataGrid(DataGrid dataGrid) {
		if (this.datagrid == null) {
			this.datagrid = dataGrid;
		} else if (this.datagrid != null){
			throw new IllegalStateException(
					"DSL File contains more then 1 ProcessingUnit type");
		}
	}

	public void setMemcached(Memcached memcached) {
		if (this.memcachedProcessingUnit == null) {
			this.memcachedProcessingUnit = memcached;
		} else if (this.memcachedProcessingUnit != null){
			throw new IllegalStateException(
					"DSL File contains more then one ProcessingUnit type");
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

	public void setCustomCommands(Map<String, Object> customCommands) {
		this.customCommands = customCommands;
	}

	public List<String> getDependsOn() {
		return dependsOn;
	}

	public void setDependsOn(List<String> dependsOn) {
		this.dependsOn = dependsOn;
	}

	public ComputeDetails getCompute() {
		return compute;
	}

	public void setCompute(ComputeDetails compute) {
		this.compute = compute;
	}
	
	public void setExtendedServicesPaths(
			LinkedList<String> extendedServicesPaths) {
		this.extendedServicesPaths = extendedServicesPaths;
	}
	
	public LinkedList<String> getExtendedServicesPaths() {
		return extendedServicesPaths;
	}
	
	
}
