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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.packaging.PackagingException;

/**
 * 
 */
public abstract class BaseApplicationScript extends Script {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseApplicationScript.class.getName());

	private Object activeObject = null;
	private Application application;

	private Object applicationDir;

	@Override
	public void setProperty(final String name, final Object value) {
		if (this.inServiceBlock) {
			try {
				handleServiceParameter(name, value);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (PackagingException e) {
				throw new RuntimeException(e);
			} catch (DSLException e) {
				throw new RuntimeException(e);
			}
		} else {
			applyPropertyToObject(this.activeObject, name, value);
		}
	}

	private void applyPropertyToObject(final Object object, final String name,
			final Object value) {
		try {
			// first check that the property exists
			BeanUtils.getProperty(object, name);
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

	private void handleServiceParameter(final String name, final Object value) 
			throws FileNotFoundException, PackagingException, DSLException  {
		if (!this.serviceBlockInitialized) {
			// first element MUST be name
			if (!"name".equals(name)) {
				throw new IllegalArgumentException(
						"The first declaraion in a service block that is part of an application MUST be its name");
			} 
			this.currentService = loadApplicationService(value.toString());
			this.serviceBlockInitialized = true;
		} else {
			applyPropertyToObject(this.currentService, name, value);
		}

	}

	private Service loadApplicationService(final String serviceName) 
			throws FileNotFoundException, PackagingException, DSLException {
		// First find the service dir
		final String serviceDirName = this.applicationDir + File.separator
				+ serviceName;
		File serviceDir = new File(serviceDirName);
		if (!serviceDir.exists() || !serviceDir.isDirectory()) {
			throw new java.lang.IllegalStateException(
					"Could not find service directory: " + serviceDir
							+ " while loading application");
		}

		// Load the service
		DSLServiceCompilationResult result = ServiceReader.getServiceFromDirectory(serviceDir);

		return result.getService();

		// execute the closure

		// swapActiveObject(closure, service);

		// add the service to the application
		// application.getServices().add(service);
	}

	/**
	 * 
	 * @param name name
	 * @param args args
	 * @return null
	 */
	public Object methodMissing(final String name, final Object args) {
		setProperty(name, args);
		return null;
		
	}

	/**
	 * 
	 * @param closure the closure
	 * @return application
	 */
	public Application application(final Closure<Object> closure) {
		this.applicationDir = this.getProperty(DSLUtils.APPLICATION_DIR);
		if (this.applicationDir == null) {
			throw new java.lang.IllegalStateException(
					"Could not read application DSL file as the application Directory was not set");
		}
		this.application = new Application();
		// if (context == null) {
		// context = new ServiceContext(service, null, null); //TODO - fix this
		// }

		this.activeObject = this.application;
		closure.call();
		return this.application;

	}

	@Override
	public void println(final Object obj) {
		logger.info(obj.toString());
	}


	private boolean inServiceBlock = false;
	private boolean serviceBlockInitialized = false;
	private Service currentService = null;

	/**
	 * 
	 * @param closure the closure
	 */
	public void service(final Closure<Object> closure) {
		if (inServiceBlock) {
			throw new java.lang.IllegalStateException(
					"Nested services are not supported");
		}

		inServiceBlock = true;
		serviceBlockInitialized = false;
		currentService = null;
		closure.call();

		if (this.currentService == null) {
			throw new IllegalArgumentException(
					"Service element must specify the service name in the first line");
		}

		// add the service to the application
		application.getServices().add(this.currentService);

		inServiceBlock = false;
		serviceBlockInitialized = false;
		currentService = null;

	}

	/**
	 * 
	 * @param closure the closure
	 * @param obj the object
	 * @return null
	 */
	protected Object swapActiveObject(final Closure<Object> closure,
			final Object obj) {
		final Object prevObject = this.activeObject;
		this.activeObject = obj;

		closure.setResolveStrategy(Closure.OWNER_ONLY);
		try {
			final Object res = closure.call();
			activeObject = prevObject;

			return res;

		} catch (Exception e) {
			System.out.println(e);
		}

		return null;
	}

}
