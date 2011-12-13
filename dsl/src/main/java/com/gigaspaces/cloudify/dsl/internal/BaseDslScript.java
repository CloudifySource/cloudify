package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
//import org.apache.commons.beanutils.PropertyUtils;
import org.openspaces.ui.BalanceGauge;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.UserInterface;
import org.openspaces.ui.WidgetGroup;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Cloud;
import com.gigaspaces.cloudify.dsl.Cloud2;
import com.gigaspaces.cloudify.dsl.CloudProvider;
import com.gigaspaces.cloudify.dsl.CloudTemplate;
import com.gigaspaces.cloudify.dsl.CloudUser;
import com.gigaspaces.cloudify.dsl.ComputeDetails;
import com.gigaspaces.cloudify.dsl.DataGrid;
import com.gigaspaces.cloudify.dsl.Memcached;
import com.gigaspaces.cloudify.dsl.MirrorProcessingUnit;
import com.gigaspaces.cloudify.dsl.PluginDescriptor;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.ServiceLifecycle;
import com.gigaspaces.cloudify.dsl.ServiceNetwork;
import com.gigaspaces.cloudify.dsl.Sla;
import com.gigaspaces.cloudify.dsl.StatefulProcessingUnit;
import com.gigaspaces.cloudify.dsl.StatelessProcessingUnit;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

public abstract class BaseDslScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseDslScript.class.getName());

	protected Object activeObject = null;
	private Object rootObject;

	@Override
	public void setProperty(final String name, final Object value) {

		if (this.activeObject == null) {
			super.setProperty(name, value);
			return;
		}
		// if(this.activeObject == null) {
		// super.setProperty(name, value);
		// }

		if (value.getClass().isArray()) {
			Object[] arr = (Object[]) value;
			if (arr.length > 1) {
				throw new IllegalArgumentException(
						"Property assignment of field: "
								+ name
								+ " received an array with more then one item: "
								+ arr);
			}
			applyPropertyToObject(this.activeObject, name, arr[0]);
		} else {
			applyPropertyToObject(this.activeObject, name, value);
		}

	}

	private boolean isProperyExistsInBean(final Object bean,
			final String propertyName) {
		if (bean == null) {
			throw new NullPointerException(
					"Got a null reference to a bean while checking if a bean has the property: "
							+ propertyName);
		}
		try {
			// first check that the property exists
			BeanUtils.getProperty(bean, propertyName);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	private void applyPropertyToObject(final Object object, final String name,
			final Object value) {

		if (!isProperyExistsInBean(object, name)) {
			throw new IllegalArgumentException("Could not find property: "
					+ name + " on Object: " + object);
		}

		try {
			// Then set it
			BeanUtils.setProperty(object, name, value);

		} catch (final Exception e) {
			throw new IllegalArgumentException("Failed   to set property "
					+ name + " of Object " + object + " to value: " + value, e);
		}

		checkForApplicationServiceBlockNameParameter(name, value);

	}

	@Override
	public Object invokeMethod(final String name, final Object arg) {

		beforeHandleInvokeMethod(name, arg);

		Object[] arr = (Object[]) arg;
		Object param = arr[0];
		// check if this is an object declaration
		if (param instanceof Closure<?>) {
			@SuppressWarnings("unchecked")
			Closure<Object> closure = (Closure<Object>) param;
			Object retval;
			try {
				retval = dslObject(name);
			} catch (DSLException e) {
				throw new IllegalArgumentException("Failed to  set: " + name, e);
			}

			if (retval != null) {
				if (this.rootObject == null) {
					this.rootObject = retval;
				}
				swapActiveObject(closure, retval);
				if (this.activeObject != null) {
					try {
						setProperty(name, retval);
					} catch (IllegalArgumentException e) {
						// this will happen every time there is a dsl object
						// declaration
						// inside something like a groovy map or list.
					}
				}
				return retval;
			}
		}

		try {
			if (handleSpecialProperty(name, arg))
				return null;
		} catch (DSLException e) {
			throw new IllegalArgumentException("Failed to set: " + name, e);
		}

		// not an object declaration
		setProperty(name, arg);
		return null;

	}

	protected void beforeHandleInvokeMethod(String name, Object arg) {
	}

	protected boolean handleSpecialProperty(String name, Object arg)
			throws DSLException {
		return false;
	}

	private static class DSLObjectInitializerData {
		private Class<?> clazz;
		private boolean allowRootNode;
		private boolean allowInternalNode;
		private String name;
		private String parentElement;

		public DSLObjectInitializerData(String name, Class<?> clazz,
				boolean allowRootNode, boolean allowInternalNode,
				String parentElement) {
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

	private static Map<String, DSLObjectInitializerData> dslObjectInitializersByName = null;// new
																							// HashMap<String,
																							// BaseDslScript.DSLObjectInitializerData>();

	private static void addObjectInitializerForClass(
			Map<String, DSLObjectInitializerData> map, Class<?> clazz) {
		CloudifyDSLEntity entityDetails = clazz
				.getAnnotation(CloudifyDSLEntity.class);

		if (entityDetails == null) {
			throw new IllegalStateException("Incorrect configuration - class "
					+ clazz.getName() + " is not a DSL entity");
		}
		map.put(entityDetails.name(),
				new DSLObjectInitializerData(entityDetails.name(),
						entityDetails.clazz(), entityDetails.allowRootNode(),
						entityDetails.allowInternalNode(), entityDetails
								.parent()));

	}

	private static Map<String, DSLObjectInitializerData> getDSLInitializers() {
		if (dslObjectInitializersByName == null) {
			dslObjectInitializersByName = new HashMap<String, BaseDslScript.DSLObjectInitializerData>();

			addObjectInitializerForClass(dslObjectInitializersByName,
					Application.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					DataGrid.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					Memcached.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					PluginDescriptor.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					Service.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					ServiceLifecycle.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					ServiceNetwork.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					StatefulProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					StatelessProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					MirrorProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					Cloud.class);

			addObjectInitializerForClass(dslObjectInitializersByName,
					Cloud2.class);

			addObjectInitializerForClass(dslObjectInitializersByName,
					CloudProvider.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					CloudUser.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					CloudTemplate.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					ComputeDetails.class);

			addObjectInitializerForClass(dslObjectInitializersByName,
					StatefulProcessingUnit.class);
			addObjectInitializerForClass(dslObjectInitializersByName,
					StatelessProcessingUnit.class);

			addObjectInitializerForClass(dslObjectInitializersByName,
					ComputeDetails.class);
			addObjectInitializerForClass(dslObjectInitializersByName, Sla.class);

			dslObjectInitializersByName.put("userInterface",
					new DSLObjectInitializerData("userInterface",
							UserInterface.class, false, true, "service"));

			dslObjectInitializersByName.put("metricGroup",
					new DSLObjectInitializerData("metricGroup",
							MetricGroup.class, false, true, "userInterface"));
			dslObjectInitializersByName.put("widgetGroup",
					new DSLObjectInitializerData("widgetGroup",
							WidgetGroup.class, false, true, "userInterface"));
			dslObjectInitializersByName.put("balanceGauge",
					new DSLObjectInitializerData("balanceGauge",
							BalanceGauge.class, false, true, "widgetGroup"));
			dslObjectInitializersByName.put("barLineChart",
					new DSLObjectInitializerData("barLineChart",
							BarLineChart.class, false, true, "widgetGroup"));

		}
		return dslObjectInitializersByName;

	}

	private Object dslObject(String name) throws DSLException {
		DSLObjectInitializerData data = getDSLInitializers().get(name);
		if (data == null) {
			return null;
		}

		if (this.activeObject == null) {
			// root node
			if (!data.isAllowRootNode()) {
				throw new DSLException("Elements of  type " + name
						+ " may not be used as the root node of a DSL");
			}
		} else {
			// internal node
			if (data.isAllowInternalNode()) {
				// check that node is nested under allowed element
				if (data.getParentElement() != null
						&& data.getParentElement().length() > 0) {
					DSLObjectInitializerData parentType = getDSLInitializers()
							.get(data.getParentElement());
					if (parentType == null) {
						throw new IllegalStateException(
								"The DSL type "
										+ name
										+ " has a declared parent type of "
										+ data.getParentElement()
										+ " which is not a known type. This should not happen.");
					}
					if (!parentType.getClazz().isAssignableFrom(
							this.activeObject.getClass())) {
						throw new DSLException("The type: " + name
								+ " may only be nested under elements of type "
								+ parentType.getName());
					}

				}
			} else {
				throw new DSLException("Elements of type: " + name
						+ " may not be placed in internal nodes");
			}

		}

		try {
			// Check if this is in extend mode. The active object should already
			// contain a value
			// for this object, simply clone it so we keep its content.
			
			if (this.activeObject !=null &&  (!(this.activeObject instanceof Application)) && isProperyExistsInBean(this.activeObject, name)) {
				Object existingPropertyValue = PropertyUtils.getProperty(this.activeObject, name);
				if (existingPropertyValue != null)
					return BeanUtils.cloneBean(existingPropertyValue);
			}
			return data.clazz.newInstance();
		} catch (InstantiationException e) {
			throw new DSLException("Failed to create new element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		} catch (IllegalAccessException e) {
			throw new DSLException("Failed to create new element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		}
		catch (InvocationTargetException e) {
			throw new DSLException("Failed to copy existing element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		} catch (NoSuchMethodException e) {
			throw new DSLException("Failed to copy existing element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		}
	}

	@Override
	public void println(final Object obj) {
		if (obj == null)
			logger.info("null");
		else
			logger.info(obj.toString());
	}

	protected void swapActiveObject(final Closure<Object> closure,
			final Object obj) {
		final Object prevObject = this.activeObject;
		this.activeObject = obj;

		closure.setResolveStrategy(Closure.OWNER_FIRST);
		closure.call();
		activeObject = prevObject;

		return;
	}

	// //////////////////////////////////////////////////////////////////////////////////
	// Special handling for service blocks embedded inside application files
	// //////////
	// //////////////////////////////////////////////////////////////////////////////////
	private void checkForApplicationServiceBlockNameParameter(
			final String propertyName, final Object propertyValue) {
		// check that we are setting the name property of a service this ia part
		// of an application
		if (this.rootObject != null
				&& this.rootObject.getClass().equals(Application.class)
				&& this.activeObject != null
				&& this.activeObject.getClass().equals(Service.class)
				&& propertyName.equals("name")) {
			final String serviceName = (String) propertyValue;
			Service service = loadApplicationService(serviceName);

			// TODO - must validate that name property was first one to be
			// applied in this service.
			try {
				BeanUtils.copyProperties(this.activeObject, service);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Failed to load service: "
						+ serviceName, e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException("Failed to load service: "
						+ serviceName, e);
			}

		}

	}

	private Service loadApplicationService(String serviceName) {
		// First find the service dir

		final String workDirectory = (String) this
				.getProperty(DSLUtils.APPLICATION_DIR);
		if (workDirectory == null) {
			throw new IllegalArgumentException(
					"Work directory was not set while parsing application file");
		}

		final String serviceDirName = workDirectory + File.separator
				+ serviceName;
		File serviceDir = new File(serviceDirName);
		if (!serviceDir.exists() || !serviceDir.isDirectory()) {
			throw new java.lang.IllegalStateException(
					"Could not find service directory: " + serviceDir
							+ " while loading application");
		}

		// Load the service
		DSLServiceCompilationResult result;
		try {
			result = ServiceReader.getServiceFromDirectory(serviceDir,
					((Application) this.rootObject).getName());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to load service: "
					+ serviceName + " while loading application", e);
		} catch (PackagingException e) {
			throw new IllegalArgumentException("Failed to load service: "
					+ serviceName + " while loading application", e);
		}
		Service service = result.getService();

		return service;

	}

}
