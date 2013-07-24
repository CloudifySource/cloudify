/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.usm.launcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.dsl.internal.debug.DebugModes;

/**********
 * Utility class that executes the DebugHook groovy class which is responsible for preparing the lifecycle debug
 * environment.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public class DebugHookInvoker {

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DebugHookInvoker.class
			.getName());

	private static boolean initialized = false;

	private static Class<?> clazz;

	private static Constructor<?> constructor;

	private static Method debugMethod;

	private static void init(final ClassLoader loader) throws ClassNotFoundException, SecurityException,
			NoSuchMethodException {
		if (initialized) {
			return;
		}

		clazz = loader.loadClass("org.cloudifysource.debug.DebugHook");
		Constructor<?>[] constructors = clazz.getConstructors();
		constructor = null;
		for (Constructor<?> aconstructor : constructors) {
			if (aconstructor.getParameterTypes().length == 2) {
				constructor = aconstructor;
				break;
			}
		}

		if (constructor == null) {
			throw new IllegalStateException("Could not find DebugHook with expected number of parameters");
		}

		debugMethod = clazz.getMethod("debug", List.class);

	}

	/*****
	 * Execute the debug hook and return the modified command line.
	 *
	 * @param context
	 *            .
	 * @param command
	 *            .
	 * @param loader
	 *            .
	 * @param mode
	 *            .
	 * @return the modified command line.
	 */
	@SuppressWarnings("unchecked")
	public List<String> setUpDebugHook(final ServiceContext context,
			final List<String> command, final ClassLoader loader, final DebugModes mode) {

		if (context == null) {
			throw new IllegalStateException("No service context object found in binding");
		}

		try {

			init(loader);

			final Object debugHookObject = constructor.newInstance(context, mode.getName());

			final Object retval = debugMethod.invoke(debugHookObject, command);

			if (retval == null) {
				throw new IllegalStateException("DebugHook returned null response");
			}

			logger.info("The Return value of the debug hook is: " + retval);
			return (List<String>) retval;

		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Could not find DebugHook class in classloader");

		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		} catch (SecurityException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException("Failed to set up debug Hook: " + e.getMessage(), e);
		}

	}

}