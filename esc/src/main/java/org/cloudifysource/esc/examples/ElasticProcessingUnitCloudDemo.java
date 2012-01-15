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
package org.cloudifysource.esc.examples;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.esm.CloudMachineProvisioningConfig;
import org.cloudifysource.esc.esm.CloudScaleOutTask;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.jclouds.compute.domain.NodeMetadata;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

import com.j_spaces.core.client.SQLQuery;

/**
 * **** A demonstration of the ESM managed data grid functionality.
 * 
 * @author barakme
 */
public final class ElasticProcessingUnitCloudDemo {

	// private static final String DEFAULT_MIN_RAM_MB = "4096";
	// private static final String DEFAULT_IMAGE_ID_RACKSPACE = "51";

	/**
	 * * Tester for the Gigaspaces JDBC functionality.
	 */
	public static final class JDBCQuery {
		/**
		 * * Runs the JDBC test.
		 * 
		 * @param locator
		 *            Locator for the Lookup Service.
		 * @param dataGridName
		 *            Name of the IMDG.
		 * @throws Exception
		 *             if the test failed.
		 */
		public static void testJDBC(final String locator, final String dataGridName) throws Exception {

			Connection conn = null;
			Statement st = null;
			try {
				Class.forName("com.j_spaces.jdbc.driver.GDriver").newInstance();
				final String url = "jdbc:gigaspaces:url:jini://*/*/" + dataGridName + "?locators=" + locator;
				conn = DriverManager.getConnection(url);
				st = conn.createStatement();
				final String query = "SELECT * FROM com.gigaspaces.rackspace.examples.Data";
				final ResultSet rs = st.executeQuery(query);

				// Iterate through the result set
				int i = 0;
				while (rs.next()) {

					System.out.println("Data [" + (i++) + "] " + rs.getString("data"));// +

				}
			} finally {
				if (st != null) {
					st.close();
				}
				if (conn != null) {
					conn.close();
				}
			}

		}

		private JDBCQuery() {

		}

	}

	// private static final String PROPERTIES_FILE = "rackspace.properties";
	private static final String PROPERTIES_FILE = "aws.properties";

	private static NodeMetadata createESMServer(final JCloudsDeployer deployer, final String name,
												final InstallationDetails details) throws IOException,
			ElasticMachineProvisioningException, InterruptedException, TimeoutException, InstallerException {

		final AgentlessInstaller installer = new AgentlessInstaller();

		final CloudScaleOutTask task = new CloudScaleOutTask(installer, details, name, deployer);

		task.run(1, 10, TimeUnit.MINUTES);

		return task.getServers().iterator().next();

	}

	/* CHECKSTYLE:OFF */

	/**
	 * ***** Create the test data.
	 * 
	 * @param i
	 *            index.
	 * @return the test data.
	 */
	public static Map<String, Object> createInfo(final long i) {
		final Map<String, Object> info = new HashMap<String, Object>();
		info.put("address", i + " Broadway");
		info.put("socialSecurity", 1232287642L + i);
		info.put("salary", 10000 + i);
		return info;
	}

	private static InstallationDetails createInstallationDetails(final CloudMachineProvisioningConfig config,
			final String targetIp) {
		final InstallationDetails details = new InstallationDetails();
		details.setLocalDir(config.getLocalDirectory());
		details.setRemoteDir(config.getRemoteDirectory());
		details.setLocator(null);
		details.setPrivateIp(null);
		details.setLus(true);
		if ((config.getKeyPair() != null) && (config.getKeyPair().length() > 0)) {
			final File keyFile = new File(details.getLocalDir(), config.getKeyFile());
			if (!keyFile.exists()) {
				throw new IllegalArgumentException("keyfile : " + keyFile.getAbsolutePath() + " not found");
			}
			details.setKeyFile(keyFile.getAbsolutePath());
		}

		return details;
	}

	private static Properties loadProperties() throws IOException {
		final Properties props = new Properties();
		final File file = new File(PROPERTIES_FILE);
		if (!file.exists()) {
			props.setProperty("user", "MY_USERNAME");
			props.setProperty("apiKey", "MY_ACCESS_KEY");
			FileOutputStream out = null;
			try {

				out = new FileOutputStream(file);
				props.store(out, "Generated properties file for ESM Demo Rackspace properties");

				throw new FileNotFoundException("Could not find properties file: " + file.getAbsolutePath()
						+ ". A default one was created, " + "please set the values according to your Rackspace account");
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (final IOException ioe) {
						// ignore
					}
				}
			}
		}

		final InputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			props.load(is);
			return props;
		} finally {
			is.close();
		}

	}

	/**
	 * The main method for the demo.
	 * 
	 * @param args
	 *            not used.
	 * @throws Exception .
	 */
	public static void main(final String[] args) throws Exception {

		final String gridName = "myElasticDataGrid";
		final String esmNodeName = "gs_esm_manager";
		final Properties props = ElasticProcessingUnitCloudDemo.loadProperties();
		final CloudMachineProvisioningConfig config = new CloudMachineProvisioningConfig(props);
		config.setMachineNamePrefix("gs_esm_gsa_");
		config.setGridName(gridName);

		// final String hardwareId = props.getProperty("hardwareId");
		// Creating a data grid on demand
		System.out.println("Deploying Data Grid..");

		final InstallationDetails details =
				ElasticProcessingUnitCloudDemo.createInstallationDetails(config,
						null);
		// Launch the head node, with the GigaSpaces Elastic Scaling Module
		// or find an existing one
		final String locator =
				ElasticProcessingUnitCloudDemo.startESMNode(esmNodeName, details,
						config);

		config.setLocalDirectory(config.getRemoteDirectory());

		// Starts the GS-UI with the locator
		// ElasticProcessingUnitRackspaceDemo.startGUI(locator);

		// Initialize the GigaSpaces Admin API with the locator for the head
		// node
		final Admin admin = new AdminFactory().addLocator(locator).createAdmin();

		final GridServiceManager gsm = admin.getGridServiceManagers().waitForAtLeastOne(30, TimeUnit.SECONDS);
		if (gsm == null) {
			System.err.println("GSM was not found!");
			System.exit(1);
		}

		// deploy web-ui
		// deployWebUI(gsm, admin);

		config.setMachineNamePrefix("gs_esm_gsa_");
		config.setLocator(locator);

		System.out.println("Deploying processing unit with config: " + config.getProperties());
		final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment(gridName)
				.maxMemoryCapacity(4, MemoryUnit.GIGABYTES)
				.memoryCapacityPerContainer(256, MemoryUnit.MEGABYTES)
				.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1, MemoryUnit.GIGABYTES).create())
				.dedicatedMachineProvisioning(config)
				);

		try {

			System.out.println("Waiting for data grid deployment completion ..");
			final Space space = pu.waitForSpace();

			while (!space.waitFor(space.getTotalNumberOfInstances(), 10, TimeUnit.SECONDS)) {
				System.out.println("Waiting for all partitions to deploy. " + "Available: "
						+ space.getNumberOfInstances() + ", " + "Required: " + space.getTotalNumberOfInstances());
				for (final String host : admin.getMachines().getHostsByAddress().keySet()) {
					System.out.println(host + "\n");

				}
			}

			System.out.println("All instances have been deployed, the Data Grid is ready for action");

			// Write some data...
			final GigaSpace gigaSpace = space.getGigaSpace();

			System.out.println("Writing 1000 objects");
			for (long i = 0; i < 1000; i++) {
				gigaSpace.write(new Data(i, "message" + i, ElasticProcessingUnitCloudDemo.createInfo(i)));
			}

			System.out.println("Reading 1000 objects");
			final Data[] d = gigaSpace.readMultiple(new SQLQuery<Data>(Data.class,
					"info.salary < 11000 and info.salary >= 10000"), Integer.MAX_VALUE);

			System.out.println("SQLQuery<Data>(Data.class, info.salary < 15000 and info.salary >= 8000) results -> ["
					+ d.length + " ]");

			System.out.println("Reading 1000 using JDBC connector objects");

			JDBCQuery.testJDBC(locator, gridName);

			pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(4, MemoryUnit.GIGABYTES).create());

			pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2, MemoryUnit.GIGABYTES).create());

			// System.out.println("Clearing...");
			// pu.undeploy();
			System.out.println("Done!");
			System.exit(0);

		} catch (final Exception ex) {
			// for debugging purposes clean the deployment in case of exception
			ex.printStackTrace();
			System.out.println("Caught exception: Undeploying... ");
			pu.undeploy();
			System.exit(1);
		}
	}

	protected static void deployWebUI(final GridServiceManager gsm, final Admin admin) {

		ProcessingUnit webUi = admin.getProcessingUnits().getProcessingUnit("gs-webui");
		if (webUi == null) {

			System.out.println("Deploying Web-UI WAR file to Service Grid");
			webUi =
					gsm.deploy(new ProcessingUnitDeployment(
							"D:/Gigaspaces/gigaspaces-xap-premium-8.0.1-ga/tools/gs-webui/gs-webui.war"));
			if (!webUi.waitFor(1, 60, TimeUnit.SECONDS)) {
				System.err.println("Web UI not deployed!");
				System.exit(1);
			}
		}

		// Lookup web UI service details
		final Map<String, Object> details =
				webUi.getInstances()[0].getServiceDetailsByServiceId().values().iterator().next().getAttributes();
		final String webHost = (String) details.get("host");
		final int port = (Integer) details.get("port");
		final String ctx = (String) details.get("context-path");
		System.out.println("Web UI successfully deployed and available at: http://" + webHost + ":" + port + ctx);
	}

	private static String startESMNode(final String name,
										final InstallationDetails details,
										final CloudMachineProvisioningConfig config)
			throws IOException, ElasticMachineProvisioningException, InterruptedException, TimeoutException, InstallerException {

		JCloudsDeployer deployer = null;
		try {
			deployer = new JCloudsDeployer(config.getProvider(), config.getUser(), config.getApiKey());

			deployer.setMinRamMegabytes((int) config.getMachineMemoryMB());
			deployer.setImageId(config.getImageId());
			deployer.setHardwareId(config.getHardwareId());
//			deployer.setSecurityGroup(config.getSecurityGroup());
//			deployer.setKeyPair(config.getKeyPair());

			NodeMetadata server = null;

			// First check if a server already exists with this name
			System.out.println("Checking if server already exists.");
			server = deployer.getServerByTag(name);
			if (server != null) {

				if (server.getPrivateAddresses().isEmpty()) {
					throw new IllegalArgumentException("A server with tag " + name
							+ " already exists, but its private adresses are not available");
				}
				final String locator = (String) server.getPrivateAddresses().toArray()[0];
				System.out.println("Server " + name + " already exists, locator = " + locator);
				return locator;
			}

			// If not, the GigaSpaces deployer creates a
			// cloud server and sets up Gigaspaces on it.
			final NodeMetadata rackspaceServer =
					ElasticProcessingUnitCloudDemo.createESMServer(deployer, name, details);

			if (rackspaceServer == null) {
				throw new IllegalStateException("Failed to start GigaSpaces server!");
			}

			// Use the private address as a locator, since we are deploying
			// from inside the Rackspace cloud.
			final String locator = (String) rackspaceServer.getPrivateAddresses().toArray()[0];

			return locator;
		} finally {
			if (deployer != null) {
				deployer.destroy();
			}
		}

	}

	private ElasticProcessingUnitCloudDemo() {

	}

	/* CHECKSTYLE:ON */
}
