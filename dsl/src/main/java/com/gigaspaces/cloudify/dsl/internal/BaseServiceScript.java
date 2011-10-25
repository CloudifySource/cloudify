package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.gigaspaces.cloudify.dsl.DataGrid;
import com.gigaspaces.cloudify.dsl.Memcached;
import com.gigaspaces.cloudify.dsl.PluginDescriptor;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.ServiceLifecycle;
import com.gigaspaces.cloudify.dsl.ServiceNetwork;
import com.gigaspaces.cloudify.dsl.Sla;
import com.gigaspaces.cloudify.dsl.StatefulProcessingUnit;
import com.gigaspaces.cloudify.dsl.StatelessProcessingUnit;
import org.openspaces.ui.BalanceGauge;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.UserInterface;
import org.openspaces.ui.WidgetGroup;

public abstract class BaseServiceScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseServiceScript.class.getName());
	// protected ServiceContext context;

	private Object activeObject = null;

	private Map<String, Method> activeMethods;
	private Service service;

	private static class DSLEntryResult {
		private Object response;
		private boolean success = false;

		public DSLEntryResult(boolean success, Object response) {
			super();
			this.success = success;
			this.response = response;
		}

		public Object getResponse() {
			return response;
		}

		public boolean isSuccess() {
			return success;
		}

	}

	@Override
	public void setProperty(final String name, final Object value) {
		DSLEntryResult result = methodMissingImpl(name, value);
		if (!result.isSuccess()) {
			super.setProperty(name, value);
		}
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		Object[] argsArray = (Object[]) args;
		if (argsArray.length == 1) {
			DSLEntryResult result = methodMissingImpl(name, argsArray[0]);
			if (result.isSuccess()) {
				return result.getResponse();
			}

		}

		// important - do not try to wrap an exception thrown from this call - 
		// groovy expects to get a missing method exception here, in which case
		// it will try to resolve the method somewhere else.
		return super.invokeMethod(name, args);
	}

	// TODO: What a mess. Rewrite the whole thing. Use the BaseDSLScript
	// instead.
	@SuppressWarnings("unchecked")
	public DSLEntryResult methodMissingImpl(final String name, final Object arg) {

		// first check if this is an object declaration
		final Object obj = createDslObject(name);
		if (obj != null) {

			if (this.activeObject != null) {
				// TODO - remove all this stuff, replace with commons-beans
				// methods
				final Collection<Method> methods = this.activeMethods.values();
				for (final Method method : methods) {
					if (method.getName().startsWith("set")
							&& (method.getParameterTypes().length == 1)
							&& (method.getParameterTypes()[0].equals(obj
									.getClass()))) {

						try {
							method.invoke(this.activeObject,
									new Object[] { obj });
						} catch (final Exception e) {
							logger.log(Level.SEVERE, "Failed to set " + name, e);
							throw new IllegalArgumentException("Failed to set "
									+ name, e);
						}
						break;
					}
				}
			}

			swapActiveObject((Closure<Object>) arg, obj);
			return new DSLEntryResult(true, obj);
		}

		// next check if this is a property assignment

		final String methodName = "set" + name.substring(0, 1).toUpperCase()
				+ name.substring(1);

		if (this.activeMethods == null) {
			return new DSLEntryResult(false, null);
		}

		final Method m = this.activeMethods.get(methodName);
		if (m != null) {
			try {
				m.invoke(this.activeObject, arg);
				return new DSLEntryResult(true, this.activeObject);
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "Failed to invoke method "
						+ methodName + ": " + e.getMessage(), e);
				throw new IllegalStateException("Failed to invoke method "
						+ methodName + " on object " + this.activeObject, e);
			}
		} else {
			// logger.severe("Method " + methodName + " not found on object: "
			// + this.activeObject);
			// throw new MissingPropertyException(methodName);
			return new DSLEntryResult(false, null);

		}

	}

	public Service service(final Closure<Object> closure) {
		this.service = new Service();
		// if (context == null) {O
		// context = new ServiceContext(service, null, null); //TODO - fix this
		// }

		swapActiveObject(closure, service);
		return service;

	}

	public ServiceLifecycle lifecycle(final Closure<Object> closure) {
		final ServiceLifecycle sl = new ServiceLifecycle();

		swapActiveObject(closure, sl);

		this.service.setLifecycle(sl);
		// this.context.getService().setLifecycle(sl);
		return sl;

	}

	public Object createDslObject(final String name) {
		if (name.equals("userInterface")) {
			return new UserInterface();
		} else if (name.equals("plugin")) {
			return new PluginDescriptor();
		} else if (name.equals("metricGroup")) {
			return new MetricGroup();
		} else if (name.equals("widgetGroup")) {
			return new WidgetGroup();
		} else if (name.equals("balanceGauge")) {
			return new BalanceGauge();
		} else if (name.equals("barLineChart")) {
			return new BarLineChart();
		} else if (name.equals("network")) {
			return new ServiceNetwork();
		} else if (name.equals("statelessProcessingUnit")) {
			return new StatelessProcessingUnit();
		} else if (name.equals("statefulProcessingUnit")) {
			return new StatefulProcessingUnit();
		} else if (name.equals("memcached")) {
			return new Memcached();
		} else if (name.equals("dataGrid")) {
			return new DataGrid();
		} else if (name.equals("sla")) {
			return new Sla();
		}

		return null;
	}

	protected Object swapActiveObject(final Closure<Object> closure,
			final Object obj) {
		final Object prevObject = this.activeObject;
		this.activeObject = obj;
		final Map<String, Method> prevMethods = this.activeMethods;
		this.activeMethods = new HashMap<String, Method>();
		final Method[] methods = this.activeObject.getClass().getMethods();
		for (final Method method : methods) {
			this.activeMethods.put(method.getName(), method);
		}

		closure.setResolveStrategy(Closure.OWNER_ONLY);
		final Object res = closure.call();
		activeObject = prevObject;
		this.activeMethods = prevMethods;
		return res;
	}

	@Override
	public void println(Object obj) {
		logger.info(obj.toString());
	}

}
