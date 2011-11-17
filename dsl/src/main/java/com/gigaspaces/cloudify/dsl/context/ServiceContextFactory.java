package com.gigaspaces.cloudify.dsl.context;

import java.io.File;
import java.util.Arrays;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.core.cluster.ClusterInfo;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class ServiceContextFactory {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceContextFactory.class.getName());
	private static Admin admin = null;
	private static ServiceContext context = null;

	/****
	 * NEVER USE THIS INSIDE THE GSC. Should only be used by external scripts.
	 * 
	 * @return A newly created service context.
	 */
	public static ServiceContext getServiceContext() {

		if (context == null) {

			// TODO - this code does not support setting a specific service file
			// name
			final ClusterInfo info = createClusterInfo();
			final File dir = new File(".");
			final File dslFile = new File(dir,
					System.getenv(CloudifyConstants.USM_ENV_SERVICE_FILE_NAME));
			final String applicationName = System.getenv(CloudifyConstants.USM_ENV_APPLICATION_NAME);
			final Service service = ServiceReader.getServiceFromFile(dslFile,
					dir, applicationName).getService();
			context = new ServiceContext();

			context.init(service, getAdmin(), new File(".").getAbsolutePath(),
					info);
		}
		return context;
	}

	private static Admin getAdmin() {
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
			final int val = Integer.parseInt(var);
			return val;
		} catch (final NumberFormatException nfe) {
			logger.severe("Failed to parse integer environment variable: "
					+ name + ". Value was: " + var + ". Using default value "
					+ defaultValue + " instead");
			return defaultValue;
		}

	}

}
