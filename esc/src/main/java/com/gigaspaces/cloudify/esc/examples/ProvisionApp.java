package com.gigaspaces.cloudify.esc.examples;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

import com.gigaspaces.cloudify.esc.esm.CloudMachineProvisioningConfig;
import com.gigaspaces.cloudify.esc.esm.CloudScaleOutTask;
import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;
import com.gigaspaces.cloudify.esc.installer.InstallationDetails;
import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.gigaspaces.cloudify.esc.jclouds.JCloudsDeployer;
import com.j_spaces.core.client.SQLQuery;

/**
 * **** A demonstration of the ESM managed data grid functionality.
 * 
 * @author seankumar
 */
public final class ProvisionApp {

	private static final String PROPERTIES_FILE = "aws.properties";

	private ProvisionApp() {

	}

	/**
	 * The main method for the demo.
	 * 
	 * @param args
	 *            not used.
	 * @throws Exception .
	 */
	public static void main(final String[] args) throws Exception {

		final int memoryPerContainer = 6144; //512
		final String gridName = "helloProc";
		final String esmNodeName = "gs_esm_manager_hello";
		final String appJar = "hello-processor.jar";
		final Properties props = ProvisionApp.loadProperties();
		final CloudMachineProvisioningConfig config = new CloudMachineProvisioningConfig(
				props);

		config.setMachineNamePrefix("gs_esm_gsa_");
		config.setGridName(gridName);
		config.setsDedicatedManagementMachines(true);

		// memory for OS and other processes
		// config.setReservedMemoryCapacityPerMachineInMB(reservedInMB);

		// final String hardwareId = props.getProperty("hardwareId");
		// Creating a data grid on demand
		System.out.println("Deploying Application ..");

		final InstallationDetails details = ProvisionApp
				.createInstallationDetails(config, null);
		// Launch the head node, with the GigaSpaces Elastic Scaling Module
		// or find an existing one
		final String locator = ProvisionApp.startManagementNode(esmNodeName, details,
				config);
		System.out.println("Connecting to server at: " + locator);

		config.setLocalDirectory(config.getRemoteDirectory());

		// Initialize the GigaSpaces Admin API with the locator for the head
		// node
		final Admin admin = new AdminFactory().addLocator(locator)
				.createAdmin();

		final GridServiceManager gsm = admin.getGridServiceManagers()
				.waitForAtLeastOne(30, TimeUnit.SECONDS);
		if (gsm == null) {
			System.err.println("GSM was not found!");
			System.exit(1);
		}

		// deploy web-ui
		// deployWebUI(gsm, admin);

		config.setMachineNamePrefix("gs_esm_gsa_");
		config.setLocator(locator);

		// System.out.println("Deploying processing unit with config: " +
		// config.getProperties());
		// System.out.println("Key file = " + config.getKeyFile());

		final ProcessingUnit pu = gsm
				.deploy(new ElasticStatefulProcessingUnitDeployment(new File(
						appJar))
						.maxMemoryCapacity(memoryPerContainer*8, MemoryUnit.MEGABYTES)
						.memoryCapacityPerContainer(memoryPerContainer, MemoryUnit.MEGABYTES)
						.highlyAvailable(true)
						.scale(new ManualCapacityScaleConfigurer()
								.memoryCapacity(memoryPerContainer*4, MemoryUnit.MEGABYTES)
								.create())
						.dedicatedMachineProvisioning(config)
						//.singleMachineDeployment()
				);

		try {

			System.out.println("Waiting for deployment completion ..");
			final Space space = pu.waitForSpace();

			while (!space.waitFor(space.getTotalNumberOfInstances(), 10,
					TimeUnit.SECONDS)) {
				System.out.println("Waiting for all partitions to deploy. "
						+ "Available: " + space.getNumberOfInstances() + ", "
						+ "Required: " + space.getTotalNumberOfInstances());
				for (final String host : admin.getMachines()
						.getHostsByAddress().keySet()) {
					System.out.println(host + "\n");

				}
			}

			System.out
					.println("All instances have been deployed. Confirming Application is ready.");

			// Write some data...
			final GigaSpace gigaSpace = space.getGigaSpace();

			System.out.println("Writing TestData object");
			for (long i = 0; i < 100; i++) {
				gigaSpace.write(new TestData(i, "message" + i, ProvisionApp
						.createInfo(i)));
			}

			System.out.println("Scaling out");
			pu.scale(new ManualCapacityScaleConfigurer()
								.memoryCapacity(memoryPerContainer*8, MemoryUnit.MEGABYTES)
								.create());
			
			System.out.println("Reading TestData objects");
			final TestData[] d = gigaSpace.readMultiple(new SQLQuery<TestData>(
					TestData.class,
					"info.salary < 11000 and info.salary >= 10000"),
					Integer.MAX_VALUE);

			for (int i = 0; i < d.length; i++) {
				System.out.println("Result Data [" + i + "] is -> " + d[i]);
			}

			System.out.println("Done Writing and reading TestData!");

			System.exit(0);

		} catch (final Exception ex) {
			// for debugging purposes clean the deployment in case of exception
			ex.printStackTrace();
			System.out.println("Caught exception: Undeploying... ");
			pu.undeploy();
			System.exit(1);
		}
	}
	/**
	 * ***** Create the test data.
	 * 
	 * @param i
	 *            index.
	 * @return the test data.
	 */
	public static Map<String, Object> createInfo(final long i) {
		final Map<String, Object> info = new HashMap<String, Object>();
		info.put("name", "Name " + i);
		info.put("address", i + " Broadway");
		info.put("salary", 10000 + i);
		return info;
	}

	private static InstallationDetails createInstallationDetails(
			final CloudMachineProvisioningConfig config, final String targetIp) {
		final InstallationDetails details = new InstallationDetails();
		details.setLocalDir(config.getLocalDirectory());
		details.setRemoteDir(config.getRemoteDirectory());
		details.setLocator(null);
		details.setPrivateIp(null);
		details.setLus(true);
		if ((config.getKeyPair() != null) && (config.getKeyPair().length() > 0)) {
			final File keyFile = new File(details.getLocalDir(),
					config.getKeyFile());
			if (!keyFile.exists()) {
				throw new IllegalArgumentException("keyfile : "
						+ keyFile.getAbsolutePath() + " not found");
			}
			details.setKeyFile(keyFile.getAbsolutePath());
		}

		return details;
	}

	private static Properties loadProperties() throws IOException {
		final Properties props = new Properties();
		final File file = new File(PROPERTIES_FILE);
		if (!file.exists()) {
			props.put("user", "MY_USERNAME");
			props.put("apiKey", "MY_ACCESS_KEY");
			FileOutputStream out = null;
			try {

				out = new FileOutputStream(file);
				props.store(out,
						"Generated properties file for ESM Demo EC2 properties");

				throw new FileNotFoundException(
						"Could not find properties file: "
								+ file.getAbsolutePath()
								+ ". A default one was created, "
								+ "please set the values according to your EC2 account");
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

		final InputStream is = new BufferedInputStream(
				new FileInputStream(file));
		try {
			props.load(is);
			return props;
		} finally {
			is.close();
		}

	}

	private static String startManagementNode(final String name,
			final InstallationDetails details,
			final CloudMachineProvisioningConfig config) throws IOException, ElasticMachineProvisioningException, InterruptedException, TimeoutException, InstallerException {

		JCloudsDeployer deployer = null;
		try {
			deployer = new JCloudsDeployer(config.getProvider(),
					config.getUser(), config.getApiKey());

			deployer.setMinRamMegabytes((int) config.getMachineMemoryMB());
			deployer.setImageId(config.getImageId());
			deployer.setHardwareId(config.getHardwareId());
			deployer.setSecurityGroup(config.getSecurityGroup());
			deployer.setKeyPair(config.getKeyPair());

			NodeMetadata server = null;

			// First check if a server already exists with this name
			System.out.println("Checking if server already exists.");
			server = deployer.getServerByTag(name);
			if (server != null && server.getState() == NodeState.PENDING) {
				throw new ElasticMachineProvisioningException(
						"A server with tag "
								+ name
								+ " already exists, but it is in pending state");
			}
			
			if (server != null && server.getState() != NodeState.TERMINATED) {
				
				if (server.getPrivateAddresses().isEmpty()) {
					throw new ElasticMachineProvisioningException(
							"A server with tag "
									+ name
									+ " already exists, but its private adresses are not available. State == " + server.getState());
				}
				final String locator = (String) server.getPrivateAddresses()
						.toArray()[0];
				System.out.println("Server " + name
						+ " already exists, locator = " + locator);
				return locator;
			}

			// If not, the GigaSpaces deployer creates a
			// cloud server and sets up Gigaspaces on it.
			final NodeMetadata createdServer = ProvisionApp.createManagementServer(
					deployer, name, details, 5, TimeUnit.MINUTES);

			// Use the private address as a locator, since we are deploying
			// from inside the EC2 cloud.
			final String locator = (String) createdServer.getPrivateAddresses()
					.toArray()[0];

			return locator;
		} finally {
			if (deployer != null) {
				deployer.destroy();
			}
		}

	}

	private static NodeMetadata createManagementServer(final JCloudsDeployer deployer,
			final String name, final InstallationDetails details, long timeout, TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException, InstallerException, FileNotFoundException {

		final AgentlessInstaller installer = new AgentlessInstaller();

		final CloudScaleOutTask task = new CloudScaleOutTask(installer,
				details, name, deployer);

		task.run(1, timeout,unit);

		return task.getServers().iterator().next();

	}



}
