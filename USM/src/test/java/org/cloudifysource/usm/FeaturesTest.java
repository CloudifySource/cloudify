package org.cloudifysource.usm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.internal.space.ServiceInstanceAttemptData;
import org.hyperic.sigar.Sigar;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.openspaces.pu.container.ProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;
import org.openspaces.pu.service.ServiceMonitors;
import org.springframework.context.ApplicationContext;

import com.j_spaces.core.IJSpace;

public class FeaturesTest {

	private static GigaSpace gigaspace;
	private static GigaSpaceConfigurer gigaSpaceConfigurer;
	private static UrlSpaceConfigurer urlSpaceConfigurer;
	private static Admin admin;

	@BeforeClass
	public static void beforeClass() {

		System.out.println("java.library.path is: " + System.getProperty("java.library.path"));
		System.setProperty("com.sun.jini.reggie.initialUnicastDiscoveryPort", "4176");
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		System.setProperty("com.gs.jini_lus.locators", "127.0.0.1:4176");

		// System.setProperty("org.hyperic.sigar.path", Environment.getHomeDirectory() + "/lib/platform/sigar");

		ClusterInfo clusterInfo = new ClusterInfo(null, 1, null, 1, null);
		urlSpaceConfigurer =
				new UrlSpaceConfigurer("/./" + CloudifyConstants.MANAGEMENT_SPACE_NAME + "?locators=127.0.0.1:"
						+ CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		final IJSpace space =
				urlSpaceConfigurer
						.clusterInfo(clusterInfo)
						.addProperty("com.j_spaces.core.container.directory_services.jini_lus.start-embedded-lus",
								"true")
						.space();
		gigaSpaceConfigurer = new GigaSpaceConfigurer(space);
		gigaspace = gigaSpaceConfigurer.gigaSpace();

		admin = new AdminFactory().discoverUnmanagedSpaces().addLocator("127.0.0.1:4176").create();

		final boolean found = admin.getLookupServices().waitFor(1, 5, TimeUnit.SECONDS);
		if (!found) {
			throw new IllegalStateException("Could not find a lookup service");
		}
		Space testSpace = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, 5, TimeUnit.SECONDS);
		if (testSpace == null) {
			throw new IllegalStateException("Could not locate management space in admin");
		}
	}

	@Before
	public void beforeTest() {
		// delete all objects currently in space
		gigaspace.clear(new Object());
		USMUtils.clearAdmin();
	}

	@Test
	public void testRetriesWithRetryLeft() throws IOException, InterruptedException {
		IntegratedProcessingUnitContainer ipuc = createContainer("classpath:/retries/META-INF/spring/pu.xml");

		try {
			ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);


			ServiceInstanceAttemptData attempt = gigaspace.read(new ServiceInstanceAttemptData(), 20000);
			Assert.assertNotNull("Expected to find attempt data in space", attempt);
			Assert.assertEquals((Integer) 2, attempt.getCurrentAttemptNumber());
			Assert.assertEquals((Integer) 1, attempt.getInstanceId());
			Assert.assertEquals("groovyError", attempt.getServiceName());
			Assert.assertEquals("default", attempt.getApplicationName());

		} finally {
			ipuc.close();
		}

	}

	@Test
	public void testRetriesWithNoRetryLeft() throws IOException, InterruptedException {
		ServiceInstanceAttemptData data = createServiceInstanceAttempDataTemplate();
		data.setCurrentAttemptNumber(2);
		gigaspace.write(data);

		IntegratedProcessingUnitContainer ipuc = createContainer("classpath:/retries/META-INF/spring/pu.xml");

		try {
			ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			waitForInstanceToReachStatus(usm, USMState.ERROR);

		} finally {
			ipuc.close();
		}

	}

	private void waitForInstanceToReachStatus(final UniversalServiceManagerBean usm, final USMState targetState)
			throws InterruptedException {
		final long start = System.currentTimeMillis();
		final long end = start + 20000;

		USMState currentState = null;
		while (System.currentTimeMillis() < end) {
			ServiceMonitors[] monitors = usm.getServicesMonitors();
			Object state = monitors[0].getMonitors().get("USM_State");
			if (state != null) {
				currentState = USMState.values()[(Integer) state];
				// System.out.println("State is: " + currentState);
				if (currentState.equals(targetState)) {
					break;
				}
			}
			// sleeping before trying monitors again.
			Thread.sleep(2000);
		}

		Assert.assertEquals(targetState, currentState);
	}

	private ServiceInstanceAttemptData createServiceInstanceAttempDataTemplate() {
		ServiceInstanceAttemptData data = new ServiceInstanceAttemptData();
		data.setApplicationName("default");
		data.setServiceName("groovyError");
		data.setGscPid(new Sigar().getPid());
		data.setInstanceId(1);
		return data;
	}

	@Test
	public void testRecoveryAfterRetry() throws IOException, InterruptedException {
		ServiceInstanceAttemptData data = createServiceInstanceAttempDataTemplate();
		data.setCurrentAttemptNumber(2);
		gigaspace.write(data);

		IntegratedProcessingUnitContainer ipuc = createContainer("classpath:/retries-recovery/META-INF/spring/pu.xml");

		try {
			ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			waitForInstanceToReachStatus(usm, USMState.RUNNING);

		} finally {
			ipuc.close();
		}



	}

	private IntegratedProcessingUnitContainer createContainer(final String classpath) throws IOException {
		IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
		// provide cluster information for the specific PU instance
		ClusterInfo clusterInfo = new ClusterInfo();
		// clusterInfo.setSchema("partitioned-sync2backup");
		clusterInfo.setNumberOfInstances(1);
		clusterInfo.setInstanceId(1);
		clusterInfo.setName("default.groovyError");
		provider.setClusterInfo(clusterInfo);

		// set the config location (override the default one - classpath:/META-INF/spring/pu.xml)
		provider.addConfigLocation(classpath);

		// Build the Spring application context and "start" it
		ProcessingUnitContainer container = provider.createContainer();
		return (IntegratedProcessingUnitContainer) container;

	}


	@AfterClass
	public static void afterClass() throws Exception {
		admin.close();

		urlSpaceConfigurer.destroy();

		System.out.println("Closed down embedded space");
	}
}
