package com.gigaspaces.cloudify.dsl.internal;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

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

	private void handleServiceParameter(String name, Object value) throws FileNotFoundException, PackagingException  {
		if (!this.serviceBlockInitialized) {
			// first element MUST be name
			if (!"name".equals(name)) {
				throw new IllegalArgumentException(
						"The first declaraion in a service block that is part of an application MUST be its name");
			} else {
				this.currentService = loadApplicationService(value.toString());
				this.serviceBlockInitialized = true;
			}
		} else {
			applyPropertyToObject(this.currentService, name, value);
		}

	}

	private Service loadApplicationService(String serviceName) throws FileNotFoundException, PackagingException {
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
		DSLServiceCompilationResult result = ServiceReader
				.getServiceFromDirectory(serviceDir);
		Service service = result.getService();

		// execute the closure

		// swapActiveObject(closure, service);

		// add the service to the application
		// application.getServices().add(service);
		return service;

	}

	public Object methodMissing(final String name, final Object args) {
		setProperty(name, args);
		return null;
		// final Object[] argsArray = (Object[]) args;
		//
		// // first check if this is an object declaration
		// final Object obj = createDslObject(name);
		// if (obj != null) {
		//
		// if (this.activeObject != null) {
		// final Collection<Method> methods = this.activeMethods.values();
		// for (final Method method : methods) {
		// if (method.getName().startsWith("set") &&
		// (method.getParameterTypes().length == 1)
		// && (method.getParameterTypes()[0].equals(obj.getClass()))) {
		//
		// try {
		// method.invoke(this.activeObject, new Object[] { obj });
		// } catch (final Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// break;
		// }
		// }
		// }
		// swapActiveObject((Closure<Object>) argsArray[0], obj);
		// return obj;
		// }
		//
		// // next check if this is a property assignment
		//
		// if (argsArray.length != 1) {
		// throw new MissingMethodException(name, Service.class, argsArray);
		// }
		//
		// final Object arg = argsArray[0];
		//
		// final String methodName = "set" + name.substring(0, 1).toUpperCase()
		// + name.substring(1);
		//
		// try {
		// final Method m = this.activeMethods.get(methodName);
		// if (m != null) {
		// m.invoke(this.activeObject, arg);
		// } else {
		// logger.severe("Method " + methodName + " not found on object: " +
		// this.activeObject);
		// throw new MissingMethodException(name, this.activeObject.getClass(),
		// new Object[0]);
		//
		// }
		// } catch (final Exception e) {
		// logger.log(Level.SEVERE, "Failed to invoke method " + methodName, e);
		// throw new IllegalStateException("Failed to invoke method " +
		// methodName
		// + " on object " + this.activeObject, e);
		// }
		//
		// return this.activeObject;
	}

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

	// public void service(String serviceName, Closure<Object> closure) {
	// // First find the service dir
	// final String serviceDirName = this.applicationDir + File.separator
	// + serviceName;
	// File serviceDir = new File(serviceDirName);
	// if (!serviceDir.exists() || !serviceDir.isDirectory()) {
	// throw new java.lang.IllegalStateException(
	// "Could not find service directory: " + serviceDir
	// + " while loading application");
	// }
	//
	// // Load the service
	// DSLServiceCompilationResult result = ServiceReader
	// .getServiceFromFile(serviceDir);
	// Service service = result.getService();
	//
	// // execute the closure
	// swapActiveObject(closure, service);
	//
	// // add the service to the application
	// application.getServices().add(service);
	//
	// }

	private boolean inServiceBlock = false;
	private boolean serviceBlockInitialized = false;
	private Service currentService = null;

	public void service(Closure<Object> closure) {
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
