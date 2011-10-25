package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

import com.gigaspaces.cloudify.dsl.Cloud;

public abstract class BaseDslScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseDslScript.class.getName());

	private Object activeObject = null;
	private Object rootObject;

	@Override
	public void setProperty(final String name, final Object value) {
		Object[] arr = (Object[]) value;
		applyPropertyToObject(this.activeObject, name, arr[0]);

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

	public Object invokeMethod(final String name, final Object arg) {
		Object[] arr = (Object[])arg;
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

	private static Map<String, DSLObjectInitializerData> dslObjectInitializersByName = null;//new HashMap<String, BaseDslScript.DSLObjectInitializerData>();

	private static Map<String, DSLObjectInitializerData> getDSLInitializers() {
		if (dslObjectInitializersByName == null) {
			dslObjectInitializersByName = new HashMap<String, BaseDslScript.DSLObjectInitializerData>();

			dslObjectInitializersByName.put("cloud",
					new DSLObjectInitializerData("cloud", Cloud.class, true,
							false, null));
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
				if (data.getParentElement() != null) {
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

	public static void main(String[] args) {

	}

}
