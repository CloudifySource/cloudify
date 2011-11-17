package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.openspaces.ui.BalanceGauge;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.UserInterface;
import org.openspaces.ui.WidgetGroup;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Cloud;
import com.gigaspaces.cloudify.dsl.DataGrid;
import com.gigaspaces.cloudify.dsl.Memcached;
import com.gigaspaces.cloudify.dsl.PluginDescriptor;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.ServiceLifecycle;
import com.gigaspaces.cloudify.dsl.ServiceNetwork;
import com.gigaspaces.cloudify.dsl.StatefulProcessingUnit;
import com.gigaspaces.cloudify.dsl.StatelessProcessingUnit;

public abstract class BaseDslScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseDslScript.class.getName());

	private Object activeObject = null;
	private Object rootObject;

	@Override
	public void setProperty(final String name, final Object value) {
		logger.info("Setting Propery: name = " + name + ", value = " + value + ", ActiveObject = " + this.activeObject);

		if(this.activeObject == null) {
			super.setProperty(name, value);
			return;
		}
		if(this.activeObject == null) {
			super.setProperty(name, value);
		}
		
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

	private void applyPropertyToObject(final Object object, final String name,
			final Object value) {
		try {
			// first check that the property exists
			BeanUtils.getProperty(object, name);
			// Then set it
			BeanUtils.setProperty(object, name, value);
		} catch (final IllegalAccessException e) {
			throw new IllegalArgumentException("Failed to set property " + name
					+ " to " + value, e);
		} catch (final InvocationTargetException e) {
			throw new IllegalArgumentException("Failed to set property " + name
					+ " to " + value, e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Property " + name
					+ " does not exist in class: "
					+ this.activeObject.getClass().getName(), e);
		}
	}

	@Override
	public Object invokeMethod(final String name, final Object arg) {
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
				throw new IllegalArgumentException("Failed to set: " + name, e);
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
						// this will happen every time there is a dsl object declaration
						// inside something like a groovy map or list.
					}
				}
				return retval;
			}
		}

		// not an object declaration
		setProperty(name, arg);
		return null;

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
					Cloud.class);

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
			return data.clazz.newInstance();
		} catch (InstantiationException e) {
			throw new DSLException("Failed to create new element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		} catch (IllegalAccessException e) {
			throw new DSLException("Failed to create new element of type "
					+ data.getName() + " with class: " + data.clazz, e);
		}
	}

	@Override
	public void println(final Object obj) {
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


}
