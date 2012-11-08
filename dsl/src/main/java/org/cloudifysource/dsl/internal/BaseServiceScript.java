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
package org.cloudifysource.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.cloudifysource.dsl.DataGrid;
import org.cloudifysource.dsl.Memcached;
import org.cloudifysource.dsl.MirrorProcessingUnit;
import org.cloudifysource.dsl.PluginDescriptor;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.dsl.ServiceNetwork;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.StatefulProcessingUnit;
import org.cloudifysource.dsl.StatelessProcessingUnit;
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
		private final Object response;
		private final boolean success;

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
	public Object invokeMethod(final String name, final Object args) {
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
		if (m == null) {
			return new DSLEntryResult(false, null);
		}
			
		try {
			m.invoke(this.activeObject, arg);
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "Failed to invoke method "
					+ methodName + ": " + e.getMessage(), e);
			throw new IllegalStateException("Failed to invoke method "
					+ methodName + " on object " + this.activeObject, e);
		}
		return new DSLEntryResult(true, this.activeObject);
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
		} 
		if (name.equals("plugin")) {
			return new PluginDescriptor();
		} 
		if (name.equals("metricGroup")) {
			return new MetricGroup();
		} 
		if (name.equals("widgetGroup")) {
			return new WidgetGroup();
		} 
		if (name.equals("balanceGauge")) {
			return new BalanceGauge();
		}
		if (name.equals("barLineChart")) {
			return new BarLineChart();
		} 
		if (name.equals("network")) {
			return new ServiceNetwork();
		}
		if (name.equals("statelessProcessingUnit")) {
			return new StatelessProcessingUnit();
		}
		if (name.equals("statefulProcessingUnit")) {
			return new StatefulProcessingUnit();
		}
		if (name.equalsIgnoreCase("mirrorProcessingUnit")) { 
			return new MirrorProcessingUnit();
		}
		if (name.equals("memcached")) {
			return new Memcached();
		}
		if (name.equals("dataGrid")) {
			return new DataGrid();
		}
		if (name.equals("sla")) {
			return new Sla();
		}

		return null;
	}

	/**
	 * 
	 * @param closure .
	 * @param obj .
	 * @return .
	 */
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
	public void println(final Object obj) {
		logger.info(obj.toString());
	}

}
