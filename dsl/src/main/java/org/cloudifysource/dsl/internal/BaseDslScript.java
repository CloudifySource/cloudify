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
package org.cloudifysource.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.AppSharedIsolationSLADescriptor;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.ComputeDetails;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.DataGrid;
import org.cloudifysource.dsl.DedicatedIsolationSLADescriptor;
import org.cloudifysource.dsl.GlobalIsolationSLADescriptor;
import org.cloudifysource.dsl.IsolationSLA;
import org.cloudifysource.dsl.Memcached;
import org.cloudifysource.dsl.MirrorProcessingUnit;
import org.cloudifysource.dsl.PluginDescriptor;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.dsl.ServiceNetwork;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.StatefulProcessingUnit;
import org.cloudifysource.dsl.StatelessProcessingUnit;
import org.cloudifysource.dsl.StorageDetails;
import org.cloudifysource.dsl.TenantSharedIsolationSLADescriptor;
import org.cloudifysource.dsl.cloud.AgentComponent;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudConfiguration;
import org.cloudifysource.dsl.cloud.CloudProvider;
import org.cloudifysource.dsl.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.dsl.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.dsl.cloud.CloudUser;
import org.cloudifysource.dsl.cloud.DeployerComponent;
import org.cloudifysource.dsl.cloud.DiscoveryComponent;
import org.cloudifysource.dsl.cloud.GridComponents;
import org.cloudifysource.dsl.cloud.OrchestratorComponent;
import org.cloudifysource.dsl.cloud.RestComponent;
import org.cloudifysource.dsl.cloud.UsmComponent;
import org.cloudifysource.dsl.cloud.WebuiComponent;
import org.cloudifysource.dsl.cloud.compute.CloudCompute;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.cloud.storage.CloudStorage;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.entry.ExecutableDSLEntryFactory;
import org.cloudifysource.dsl.entry.ExecutableEntriesMap;
import org.cloudifysource.dsl.scalingrules.HighThresholdDetails;
import org.cloudifysource.dsl.scalingrules.LowThresholdDetails;
import org.cloudifysource.dsl.scalingrules.ScalingRuleDetails;
import org.cloudifysource.dsl.statistics.PerInstanceStatisticsDetails;
import org.cloudifysource.dsl.statistics.ServiceStatisticsDetails;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.openspaces.ui.BalanceGauge;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.UserInterface;
import org.openspaces.ui.WidgetGroup;

/*************
 * Base class for DSL files.
 *
 * @author barakme
 * @since 1.0
 *
 */
public abstract class BaseDslScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(BaseDslScript.class.getName());

	/********
	 * DSL property indicating extension of recipe.
	 */
	public static final String EXTEND_PROPERTY_NAME = "extend";

	private Set<String> processingUnitTypes;
	private String processingUnitType;

	protected Object activeObject = null;
	private Object rootObject;
	private int propertyCounter;

	private Set<String> usedProperties = new HashSet<String>();

	/********
	 * syntactic sigar for an empty list that process locator implementations can use to specify an empty process IDs
	 * list.
	 */
	public static final List<Long> NO_PROCESS_LOCATORS = new LinkedList<Long>();

	// DSL Initializer meta data
	private static Map<String, DSLObjectInitializerData> dslObjectInitializersByName = null;

	@Override
	public void setProperty(final String name, final Object value) {

		if (this.activeObject == null) {
			super.setProperty(name, value);
			return;
		}

		if (value.getClass().isArray()) {
			final Object[] arr = (Object[]) value;
			if (arr.length > 1) {
				throw new IllegalArgumentException("Property assignment of field: " + name
						+ " received an array with more then one item: " + Arrays.toString(arr));
			}
			applyPropertyToObject(this.activeObject, name, arr[0]);
		} else {
			applyPropertyToObject(this.activeObject, name, value);
		}

	}

	private boolean isDuplicatePropertyAllowed(final Object value) {
		// Application allows duplicate service values.
		return this.activeObject instanceof Application && value instanceof Service;
	}

	private static boolean isProperyExistsInBean(final Object bean, final String propertyName) {
		if (bean == null) {
			throw new NullPointerException("Got a null reference to a bean while checking if a bean has the property: "
					+ propertyName);
		}
		try {
			// first check that the property exists
			BeanUtils.getProperty(bean, propertyName);
			return true;
		} catch (final Exception e) {
			return false;
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void applyPropertyToObject(final Object object, final String name, final Object value) {

		if (!isProperyExistsInBean(object, name)) {
			throw new IllegalArgumentException("Could not find property: " + name + " on Object: " + object);
		}

		// Check for duplicate properties.
		if (this.usedProperties == null) {
			throw new IllegalArgumentException("used properties can not be null. Property: "
					+ name + ", Value: " + value.toString()
					+ ", Active object: " + this.activeObject);
		}
		if (this.usedProperties.contains(name)) {
			if (!isDuplicatePropertyAllowed(value)) {
				throw new IllegalArgumentException("Property duplication was found: Property "
						+ name + " is define" + "d more than once.");
			}
		}

		this.usedProperties.add(name);
		Object convertedValue = null;
		try {
			convertedValue = convertValueToExecutableDSLEntryIfNeeded(getDSLFile().getParentFile(),
					object, name, value);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("BeanUtils.setProperty(object=" + object
						+ ",name=" + name + ",value=" + convertedValue
						+ ",value.getClass()=" + convertedValue.getClass());
			}

			// Check if writable
			if (!PropertyUtils.isWriteable(object, name)) {
				throw new IllegalArgumentException("Field " + name + " in object of type: "
						+ object.getClass().getName() + " is not writable");
			}

			// If value is a map, merge with existing map
			final Object currentValue = PropertyUtils.getProperty(object, name);
			if (currentValue != null
					&& currentValue instanceof Map<?, ?>
					&& convertedValue != null
					&& convertedValue instanceof Map<?, ?>) {

				final Map<Object, Object> currentMap = (Map<Object, Object>) currentValue;
				currentMap.putAll((Map<Object, Object>) convertedValue);

			} else if (PropertyUtils.getPropertyType(object, name).isEnum() && value instanceof String) {
				final Class enumClass = PropertyUtils.getPropertyType(object, name);
				final Enum enumValue = Enum.valueOf(enumClass, (String) value);
				BeanUtils.setProperty(object, name, enumValue);
			} else {
				// Then set it
				BeanUtils.setProperty(object, name, convertedValue);
			}
		} catch (final DSLValidationException e) {
			throw new DSLValidationRuntimeException(e);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Failed to set property " + name + " of Object " + object
					+ " to value: " + value, e);
		}

		checkForApplicationServiceBlockNameParameter(name, value);

	}

	/**
	 * Convert the value to an ExecutableDSLEntry object if object's property type is ExecutableDSLEntry or
	 * ExecutableEntriesMap. Returns value otherwise.
	 *
	 * @param workDirectory
	 *            workDirectory
	 * @param object
	 *            object
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 * @return The converted object
	 * @throws IllegalAccessException
	 *             IllegalAccessException
	 * @throws InvocationTargetException
	 *             InvocationTargetException
	 * @throws NoSuchMethodException
	 *             NoSuchMethodException
	 * @throws DSLValidationException
	 *             DSLValidationException
	 */
	public static Object convertValueToExecutableDSLEntryIfNeeded(final File workDirectory,
			final Object object, final String name, final Object value)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLValidationException {

		final PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(object, name);
		final Class<?> propertyType = descriptor.getPropertyType();
		if (propertyType.equals(ExecutableDSLEntry.class)) {
			return ExecutableDSLEntryFactory.createEntry(value, name, workDirectory);
		} else if (propertyType.equals(ExecutableEntriesMap.class)) {
			return ExecutableDSLEntryFactory.createEntriesMap(value, name, workDirectory);

		} else {
			return value;
		}
	}

	private File getDSLFile() {
		return new File((String) this.getBinding().getVariable(DSLUtils.DSL_FILE_PATH_PROPERTY_NAME));
	}

	private boolean isValidateObjects() {
		return (Boolean) this.getBinding().getVariable(DSLUtils.DSL_VALIDATE_OBJECTS_PROPERTY_NAME);
	}

	@Override
	public Object invokeMethod(final String name, final Object arg) {

		beforeHandleInvokeMethod(name, arg);

		final Object[] arr = (Object[]) arg;
		final Object param = arr[0];
		// check if this is an object declaration
		if (param instanceof Closure<?>) {
			@SuppressWarnings("unchecked")
			final Closure<Object> closure = (Closure<Object>) param;
			Object retval;
			try {
				retval = dslObject(name);
			} catch (final DSLException e) {
				throw new IllegalArgumentException("Failed to  set: " + name, e);
			}

			if (retval != null) {
				if (this.rootObject == null) {
					this.rootObject = retval;
				}
				swapActiveObject(closure, retval);
				if (isValidateObjects()) {
					try {
						validateObject(retval);
					} catch (final DSLValidationException e) {
						throw new DSLValidationRuntimeException(e);
					}
				}
				if (this.activeObject != null) {
					try {
						setProperty(name, retval);
					} catch (final IllegalArgumentException e) {
						// this will happen every time there is a dsl object
						// declaration
						// inside something like a groovy map or list.
						this.usedProperties.remove(name);
					}
				}
				return retval;
			}
		}

		try {
			if (handleSpecialProperty(name, arg)) {
				return null;
			}
		} catch (final DSLException e) {
			throw new IllegalArgumentException("Failed to set " + name + ": " + e.getMessage(), e);
		}

		// not an object declaration
		setProperty(name, arg);
		return null;

	}

	/*************
	 * Loads an object from an external file.
	 *
	 * @param fileName
	 *            the filename, relative to the current file.
	 * @return the object.
	 */
	public Object load(final String fileName) {

		final String dslFilePath = (String) getProperty(ServiceReader.DSL_FILE_PATH_PROPERTY_NAME);
		if (dslFilePath == null) {
			throw new IllegalStateException("No dsl file path present in binding context");
		}
		final File activeServiceDirectory = new File(dslFilePath).getParentFile();
		final File externalFile = new File(activeServiceDirectory, fileName);
		if (!externalFile.exists()) {
			throw new IllegalArgumentException("While processing DSL file, could not find file to load " + fileName);
		}

		if (!externalFile.isFile()) {
			throw new IllegalArgumentException("While processing DSL file, could not load file " + fileName
					+ " as it is not a file");
		}

		Object result;
		try {
			result = readExternalDSLFile(externalFile);
			if (result instanceof Closure<?>) {
				final Closure<?> closure = (Closure<?>) result;
				closure.setDelegate(this);
				closure.setResolveStrategy(Closure.DELEGATE_ONLY);
			}
			return result;
		} catch (final DSLException e) {
			throw new IllegalArgumentException("Failed to load external DSL file: " + fileName, e);
		}

	}

	private Object readExternalDSLFile(final File externalFile)
			throws DSLException {
		@SuppressWarnings("unchecked")
		final Map<Object, Object> currentVars = this.getBinding().getVariables();

		final DSLReader dslReader = new DSLReader();
		dslReader.setBindingVariables(currentVars);
		dslReader.setAdmin(null);
		dslReader.setClusterInfo(null);
		dslReader.setContext(null);
		dslReader.setCreateServiceContext(false);
		dslReader.setDslFile(externalFile);
		dslReader.setPropertiesFileName(null);

		final Object result = dslReader.readDslEntity(Object.class);

		return result;
	}

	private void validateObject(final Object obj)
			throws DSLValidationException {

		final Method[] methods = obj.getClass().getDeclaredMethods();
		for (final Method method : methods) {
			if (method.getAnnotation(DSLValidation.class) != null) {
				final boolean accessible = method.isAccessible();
				try {
					@SuppressWarnings("unchecked")
					final Map<Object, Object> currentVars = this.getBinding().getVariables();
					final DSLValidationContext validationContext = new DSLValidationContext();
					validationContext.setFilePath((String) currentVars.get(DSLUtils.DSL_FILE_PATH_PROPERTY_NAME));
					method.setAccessible(true);
					method.invoke(obj, validationContext);

				} catch (final InvocationTargetException e) {
					throw new DSLValidationException(e.getTargetException().getMessage(), e.getTargetException());
				} catch (final Exception e) {
					throw new DSLValidationException("Failed to execute DSL validation: " + e.getMessage(), e);

				} finally {
					method.setAccessible(accessible);
				}
			}
		}

	}

	private void beforeHandleInvokeMethod(final String name, final Object arg) {

		if (name.equals("service")) {
			propertyCounter = 0;
		} else {
			propertyCounter++;
		}
	}

	private boolean handleSpecialProperty(final String name, final Object arg)
			throws DSLException {
		Object localArg = arg;
		if (name.equals(EXTEND_PROPERTY_NAME)) {
			if (propertyCounter > 1) {
				throw new DSLException(EXTEND_PROPERTY_NAME + " must be first inside the service block");
			}
			if (arg != null && arg.getClass().isArray()) {
				final Object[] arr = (Object[]) arg;
				if (arr.length != 1) {
					throw new DSLException(EXTEND_PROPERTY_NAME + " property must be a single string");
				}
				localArg = ((Object[]) arg)[0];
			}
			if (!(localArg instanceof String)) {
				throw new DSLException(EXTEND_PROPERTY_NAME + " property must be a string");
			}
			if (!(this.activeObject instanceof Service)) {
				throw new DSLException(EXTEND_PROPERTY_NAME + " property can only be used on a service");
			}
			final String extendServicePath = (String) localArg;
			try {

				File extendedServiceAbsPath = new File(extendServicePath);

				final RecipePathResolver resolver = new RecipePathResolver();
				// Extract the current service directory
				final String dslFilePath = (String) getProperty(ServiceReader.DSL_FILE_PATH_PROPERTY_NAME);
				if (dslFilePath == null) {
					throw new IllegalStateException("No dsl file path present in binding context");
				}
				final File activeServiceDirectory = new File(dslFilePath).getParentFile();
				resolver.setCurrentDirectory(activeServiceDirectory);
				if (resolver.resolveService(extendedServiceAbsPath)) {
					extendedServiceAbsPath = resolver.getResolved();
				} else {
					throw new DSLException("could not find extended service in paths "
							+ StringUtils.join(resolver.getPathsLooked().toArray(), ", "));
				}

				if (logger.isLoggable(Level.FINER)) {
					logger.finer("reading extended service file [" + extendedServiceAbsPath + "]");
				}
				// Read the extended service
				final Service baseService = readServiceToExtend(extendedServiceAbsPath);
				// ServiceReader.readService(extendedServiceAbsPath);

				// Populate the current service with the extended service
				BeanUtils.copyProperties(this.activeObject, baseService);
				final Service activeService = (Service) activeObject;
				// Add extended service to the extension list
				activeService.getExtendedServicesPaths().addFirst(extendedServiceAbsPath.getAbsolutePath());
				return true;
			} catch (final IllegalAccessException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			} catch (final InvocationTargetException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			}
		}
		return false;
	}

	private Service readServiceToExtend(final File serviceFileToExtend)
			throws DSLException {
		@SuppressWarnings("unchecked")
		final Map<Object, Object> currentVars = this.getBinding().getVariables();

		final DSLReader dslReader = new DSLReader();
		dslReader.setBindingVariables(currentVars);
		dslReader.setAdmin(null);
		dslReader.setClusterInfo(null);
		dslReader.setContext(null);
		dslReader.setCreateServiceContext(false);
		if (serviceFileToExtend.isDirectory()) {
			dslReader.setWorkDir(serviceFileToExtend);
		} else {
			dslReader.setDslFile(serviceFileToExtend);
		}
		dslReader.setDslFileNameSuffix(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
		// dslReader.setLoadUsmLib(true)
		dslReader.setPropertiesFileName(null);

		final Service service = dslReader.readDslEntity(Service.class);

		return service;
	}

	/**
	 *
	 *
	 */
	public static class DSLObjectInitializerData {

		private final Class<?> clazz;
		private final boolean allowRootNode;
		private final boolean allowInternalNode;
		private final String name;
		private final String parentElement;

		public DSLObjectInitializerData(final String name, final Class<?> clazz, final boolean allowRootNode,
				final boolean allowInternalNode, final String parentElement) {
			super();
			this.name = name;
			this.clazz = clazz;
			this.allowRootNode = allowRootNode;
			this.allowInternalNode = allowInternalNode;
			this.parentElement = parentElement;
		}

		public String getParentElement() {
			return parentElement;
		}

		public boolean isAllowRootNode() {
			return allowRootNode;
		}

		public boolean isAllowInternalNode() {
			return allowInternalNode;
		}

		public Class<?> getClazz() {
			return clazz;
		}

		public String getName() {
			return name;
		}

	}

	private static void addObjectInitializerForClass(final Map<String, DSLObjectInitializerData> map,
			final Class<?> clazz) {
		final CloudifyDSLEntity entityDetails = clazz.getAnnotation(CloudifyDSLEntity.class);

		if (entityDetails == null) {
			throw new IllegalStateException("Incorrect configuration - class " + clazz.getName()
					+ " is not a DSL entity");
		}
		map.put(entityDetails.name(), new DSLObjectInitializerData(entityDetails.name(), entityDetails.clazz(),
				entityDetails.allowRootNode(), entityDetails.allowInternalNode(), entityDetails.parent()));

	}

	/***********
	 * Returns the DSL Meta-data required to translate DSL elements into POJOs.
	 *
	 * @return DSL meta-data.
	 */
	public static synchronized Map<String, DSLObjectInitializerData> getDSLInitializers() {
		if (dslObjectInitializersByName == null) {
			dslObjectInitializersByName = new HashMap<String, BaseDslScript.DSLObjectInitializerData>();

			addObjectInitializerForClass(dslObjectInitializersByName, Application.class);
			addObjectInitializerForClass(dslObjectInitializersByName, Service.class);
			addObjectInitializerForClass(dslObjectInitializersByName, PluginDescriptor.class);
			addObjectInitializerForClass(dslObjectInitializersByName, ServiceNetwork.class);

			addObjectInitializerForClass(dslObjectInitializersByName, DataGrid.class);
			addObjectInitializerForClass(dslObjectInitializersByName, Memcached.class);
			addObjectInitializerForClass(dslObjectInitializersByName, ServiceLifecycle.class);
			addObjectInitializerForClass(dslObjectInitializersByName, StatefulProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName, StatelessProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName, MirrorProcessingUnit.class);

			addObjectInitializerForClass(dslObjectInitializersByName, Cloud.class);
			addObjectInitializerForClass(dslObjectInitializersByName, CloudProvider.class);
			addObjectInitializerForClass(dslObjectInitializersByName, CloudUser.class);
			addObjectInitializerForClass(dslObjectInitializersByName, ComputeTemplate.class);
			addObjectInitializerForClass(dslObjectInitializersByName, CloudCompute.class);
			addObjectInitializerForClass(dslObjectInitializersByName, CloudConfiguration.class);
			addObjectInitializerForClass(dslObjectInitializersByName, ComputeDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, CloudStorage.class);
			addObjectInitializerForClass(dslObjectInitializersByName, StorageTemplate.class);
			addObjectInitializerForClass(dslObjectInitializersByName, StorageDetails.class);

			addObjectInitializerForClass(dslObjectInitializersByName, ComputeDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, Sla.class);

			dslObjectInitializersByName.put("userInterface", new DSLObjectInitializerData("userInterface",
					UserInterface.class, true, true, "service"));

			dslObjectInitializersByName.put("metricGroup", new DSLObjectInitializerData("metricGroup",
					MetricGroup.class, false, true, "userInterface"));
			dslObjectInitializersByName.put("widgetGroup", new DSLObjectInitializerData("widgetGroup",
					WidgetGroup.class, false, true, "userInterface"));
			dslObjectInitializersByName.put("balanceGauge", new DSLObjectInitializerData("balanceGauge",
					BalanceGauge.class, false, true, "widgetGroup"));
			dslObjectInitializersByName.put("barLineChart", new DSLObjectInitializerData("barLineChart",
					BarLineChart.class, false, true, "widgetGroup"));

			addObjectInitializerForClass(dslObjectInitializersByName, ScalingRuleDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, HighThresholdDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, LowThresholdDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, ServiceStatisticsDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, PerInstanceStatisticsDetails.class);

			addObjectInitializerForClass(dslObjectInitializersByName, IsolationSLA.class);
			addObjectInitializerForClass(dslObjectInitializersByName, GlobalIsolationSLADescriptor.class);
			addObjectInitializerForClass(dslObjectInitializersByName, TenantSharedIsolationSLADescriptor.class);
			addObjectInitializerForClass(dslObjectInitializersByName, AppSharedIsolationSLADescriptor.class);
			addObjectInitializerForClass(dslObjectInitializersByName, DedicatedIsolationSLADescriptor.class);

			addObjectInitializerForClass(dslObjectInitializersByName, GridComponents.class);
			addObjectInitializerForClass(dslObjectInitializersByName, OrchestratorComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, DiscoveryComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, DeployerComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, WebuiComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, UsmComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, RestComponent.class);
			addObjectInitializerForClass(dslObjectInitializersByName, AgentComponent.class);

			addObjectInitializerForClass(dslObjectInitializersByName, CloudTemplateInstallerConfiguration.class);

		}
		return dslObjectInitializersByName;

	}

	private Object dslObject(final String name)
			throws DSLException {
		final DSLObjectInitializerData data = getDSLInitializers().get(name);
		if (data == null) {
			return null;
		}

		if (isDuplicateProcessingUnit(name)) {
			throw new DSLException("There may only be one type of processing unit defined. Found more than one: "
					+ "[" + name + ", " + this.processingUnitType + "]");
		}

		if (this.activeObject == null) {
			// root node
			if (!data.isAllowRootNode()) {
				throw new DSLException("Elements of  type " + name + " may not be used as the root node of a DSL");
			}
		} else {
			// internal node
			if (data.isAllowInternalNode()) {
				// check that node is nested under allowed element
				final String parentElement = data.getParentElement();
				if (parentElement != null && !parentElement.isEmpty()) {
					final DSLObjectInitializerData parentType = getDSLInitializers().get(parentElement);
					if (parentType == null) {
						throw new IllegalStateException("The DSL type " + name + " has a declared parent type of "
								+ parentElement + " which is not a known type. This should not happen.");
					}
					if (!parentType.getClazz().isAssignableFrom(this.activeObject.getClass())) {
						throw new DSLException("The type: " + name + " may only be nested under elements of type "
								+ parentType.getName());
					}

				}
			} else {
				throw new DSLException("Elements of type: " + name + " may not be placed in internal nodes");
			}

		}

		try {
			// Check if this is in extend mode. The active object should already
			// contain a value
			// for this object, simply clone it so we keep its content.

			if (this.activeObject != null && !(this.activeObject instanceof Application)
					&& isProperyExistsInBean(this.activeObject, name)) {
				final Object existingPropertyValue = PropertyUtils.getProperty(this.activeObject, name);
				if (existingPropertyValue != null) {
					return BeanUtils.cloneBean(existingPropertyValue);
				}
			}
			return data.clazz.newInstance();
		} catch (final InstantiationException e) {
			throw new DSLException("Failed to create new element of type " + data.getName() + " with class: "
					+ data.clazz, e);
		} catch (final IllegalAccessException e) {
			throw new DSLException("Failed to create new element of type " + data.getName() + " with class: "
					+ data.clazz, e);
		} catch (final InvocationTargetException e) {
			throw new DSLException("Failed to copy existing element of type " + data.getName() + " with class: "
					+ data.clazz, e);
		} catch (final NoSuchMethodException e) {
			throw new DSLException("Failed to copy existing element of type " + data.getName() + " with class: "
					+ data.clazz, e);
		}
	}

	// Only one of stateless/stateful/lifecycle may be set
	private boolean isDuplicateProcessingUnit(final String name) {

		final Set<String> types = getProcessingUnitTypes();
		if (types.contains(name)) {
			if (StringUtils.isEmpty(this.processingUnitType)) {
				this.processingUnitType = name;
			} else {
				return true;
			}
		}
		return false;
	}

	private Set<String> getProcessingUnitTypes() {

		if (this.processingUnitTypes == null) {
			this.processingUnitTypes = new HashSet<String>();
			this.processingUnitTypes.add("lifecycle");
			this.processingUnitTypes.add("statefulProcessingUnit");
			this.processingUnitTypes.add("statelessProcessingUnit");
			this.processingUnitTypes.add("dataGrid");
			this.processingUnitTypes.add("mirrorProcessingUnit");
		}

		return this.processingUnitTypes;
	}

	@Override
	public void println(final Object obj) {
		if (obj == null) {
			logger.info("null");
		} else {
			logger.info(obj.toString());
		}
	}

	private void swapActiveObject(final Closure<Object> closure, final Object obj) {
		final Object prevObject = this.activeObject;
		final Set<String> prevSet = this.usedProperties;

		this.activeObject = obj;
		this.usedProperties = new HashSet<String>();

		closure.setResolveStrategy(Closure.OWNER_FIRST);
		closure.call();

		activeObject = prevObject;
		this.usedProperties = prevSet;
	}

	// //////////////////////////////////////////////////////////////////////////////////
	// Special handling for service blocks embedded inside application files
	// //////////
	// //////////////////////////////////////////////////////////////////////////////////
	private void checkForApplicationServiceBlockNameParameter(final String propertyName, final Object propertyValue) {
		// check that we are setting the name property of a service this ia part
		// of an application
		if (this.rootObject != null && this.rootObject.getClass().equals(Application.class)
				&& this.activeObject != null && this.activeObject.getClass().equals(Service.class)
				&& propertyName.equals("name")) {
			final String serviceName = (String) propertyValue;
			final Service service = loadApplicationService(serviceName);
			// Override service name with application settings for it
			service.setName(serviceName);
			// TODO - must validate that name property was first one to be
			// applied in this service.
			try {
				BeanUtils.copyProperties(this.activeObject, service);
			} catch (final IllegalAccessException e) {
				throw new IllegalArgumentException("Failed to load service: " + serviceName, e);
			} catch (final InvocationTargetException e) {
				throw new IllegalArgumentException("Failed to load service: " + serviceName, e);
			}

		}

	}

	@SuppressWarnings("unchecked")
	private Service loadApplicationService(final String serviceName) {
		// First find the service dir

		final String workDirectory = (String) this.getProperty(DSLUtils.APPLICATION_DIR);
		if (workDirectory == null) {
			throw new IllegalArgumentException("Work directory was not set while parsing application file");
		}

		final String serviceDirName = workDirectory + File.separator + serviceName;
		final File serviceDir = new File(serviceDirName);
		if (!serviceDir.exists() || !serviceDir.isDirectory()) {
			throw new java.lang.IllegalStateException("Could not find service directory: " + serviceDir
					+ " while loading application");
		}

		// Load the service
		DSLServiceCompilationResult result;
		try {
			final Object applicationProperties = getBinding().getVariables().get(DSLUtils.DSL_PROPERTIES);
			Map<String, Object> applicationPropertiesMap = null;
			if (applicationProperties != null) {
				if (applicationProperties instanceof Map) {
					applicationPropertiesMap = (Map<String, Object>) applicationProperties;
				} else {
					throw new DSLException("applicationProperties must be a map.");
				}
			}
			result = ServiceReader.getApplicationServiceFromDirectory(serviceDir, applicationPropertiesMap);

		} catch (final DSLException e) {
			throw new IllegalArgumentException("Failed to load service: " + serviceName
					+ " while loading application: " + e.getMessage(), e);
		}
		final Service service = result.getService();

		return service;

	}

}