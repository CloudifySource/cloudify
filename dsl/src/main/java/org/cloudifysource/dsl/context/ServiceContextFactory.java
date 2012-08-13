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
package org.cloudifysource.dsl.context;

import java.io.File;
import java.util.Arrays;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.context.ServiceContextImpl;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.core.cluster.ClusterInfo;

/***************
 * A factory class used to set up a ServiceContext from external classes. This factory should never be used inside a
 * service recipe - recipes already have the 'context' variable injected to them automatically. External Groovy scripts
 * that require access to the CLoudify Service context may use this factory to access it. Using this factory inside a
 * service file will not work, as it relies on environment variables that cloudify adds to commands it launches.
 * 
 * @author barakme
 * 
 */
public final class ServiceContextFactory {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceContextFactory.class.getName());
	private static Admin admin = null;
	private static ServiceContext context = null;

	/*****
	 * Private constructor to avoid initialization.
	 */
	private ServiceContextFactory() {

	}

	/****
	 * NEVER USE THIS INSIDE THE GSC. Should only be used by external scripts.
	 * 
	 * @return A newly created service context.
	 */
	public static synchronized ServiceContext getServiceContext() {

		if (context == null) {

			// TODO - this code does not support setting a specific service file
			// name
			final ClusterInfo info = createClusterInfo();
			final File dir = new File(".");
			final File dslFile = new File(dir,
					System.getenv(CloudifyConstants.USM_ENV_SERVICE_FILE_NAME));
			Service service;
			try {
				service = ServiceReader.readService(dslFile);
			} catch (PackagingException e) {
				throw new IllegalArgumentException("Failed to read service", e);
			} catch (DSLException e) {
				throw new IllegalArgumentException("Failed to read service", e);
			}
			ServiceContextImpl newContext = new ServiceContextImpl(info, new File(".").getAbsolutePath());

			// TODO - this code assumes running code only from a GSC. Test-recipe will not work here!
			newContext.init(service, getAdmin(),
					info);
			context = newContext;
		}
		return context;
	}

	private static synchronized Admin getAdmin() {
		if (admin != null) {
			return admin;
		}

		final AdminFactory factory = new AdminFactory();
		factory.useDaemonThreads(true);
		admin = factory.createAdmin();

		logger.info("Created new Admin Object with groups: "
				+ Arrays.toString(admin.getGroups()) + " and Locators: "
				+ Arrays.toString(admin.getLocators()));

		return admin;
	}

	private static ClusterInfo createClusterInfo() {
		final ClusterInfo info = new ClusterInfo();

		info.setInstanceId(getIntEnvironmentVariable(
				CloudifyConstants.USM_ENV_INSTANCE_ID, 1));
		info.setName(getEnvironmentVariable(
				CloudifyConstants.USM_ENV_CLUSTER_NAME, "USM"));
		info.setNumberOfInstances(getIntEnvironmentVariable(
				CloudifyConstants.USM_ENV_NUMBER_OF_INSTANCES, 1));

		return info;

	}

	private static String getEnvironmentVariable(final String name,
			final String defaultValue) {
		final String var = System.getenv(name);
		if (var == null) {
			logger.warning("Could not find environment variable: " + name
					+ ". Using default value: " + defaultValue + " instead.");
			return defaultValue;
		}

		return var;
	}

	private static int getIntEnvironmentVariable(final String name,
			final int defaultValue) {

		final String var = getEnvironmentVariable(name, "" + defaultValue);
		try {
			return Integer.parseInt(var);
		} catch (final NumberFormatException nfe) {
			logger.severe("Failed to parse integer environment variable: "
					+ name + ". Value was: " + var + ". Using default value "
					+ defaultValue + " instead");
			return defaultValue;
		}

	}

}
