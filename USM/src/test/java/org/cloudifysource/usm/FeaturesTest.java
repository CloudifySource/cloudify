package org.cloudifysource.usm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.shutdown.DefaultProcessKiller;
import org.cloudifysource.utilitydomain.data.ServiceInstanceAttemptData;
import org.cloudifysource.utilitydomain.openspaces.OpenspacesConstants;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.properties.BeanLevelProperties;
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
	private static final String DEBUG_LOCK_FILE_PATH = "target/test-classes/debug/ext/.cloudify_debugging.lock";;

	@BeforeClass
	public static void beforeClass() {

		System.out.println("java.library.path is: " + System.getProperty("java.library.path"));
		System.setProperty("com.sun.jini.reggie.initialUnicastDiscoveryPort", "4176");
		System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		System.setProperty("com.gs.jini_lus.locators", "127.0.0.1:4176");

		// System.setProperty("org.hyperic.sigar.path", Environment.getHomeDirectory() + "/lib/platform/sigar");

		final ClusterInfo clusterInfo = new ClusterInfo(null, 1, null, 1, null);
		urlSpaceConfigurer =
				new UrlSpaceConfigurer("/./" + CloudifyConstants.MANAGEMENT_SPACE_NAME + "?locators=127.0.0.1:"
						+ OpenspacesConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
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
		final Space testSpace = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, 5, TimeUnit.SECONDS);
		if (testSpace == null) {
			throw new IllegalStateException("Could not locate management space in admin");
		}
	}

	@Before
	public void beforeTest() {
		// delete all objects currently in space
		gigaspace.clear(new Object());
		USMUtils.shutdownAdmin();
	}

	@Ignore
	@Test
	public void testRetriesWithRetryLeft() throws IOException, InterruptedException {
		final IntegratedProcessingUnitContainer ipuc = createContainer("classpath:/retries/META-INF/spring/pu.xml");

		try {
			final ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			final ServiceInstanceAttemptData attempt = gigaspace.read(new ServiceInstanceAttemptData(), 20000);
			Assert.assertNotNull("Expected to find attempt data in space", attempt);
			Assert.assertEquals((Integer) 2, attempt.getCurrentAttemptNumber());
			Assert.assertEquals((Integer) 1, attempt.getInstanceId());
			Assert.assertEquals("groovyError", attempt.getServiceName());
			Assert.assertEquals("default", attempt.getApplicationName());

		} finally {
			ipuc.close();
		}

	}

	@Ignore
	@Test
	public void testRetriesWithNoRetryLeft() throws IOException, InterruptedException {
		final ServiceInstanceAttemptData data = createServiceInstanceAttempDataTemplate();
		data.setCurrentAttemptNumber(2);
		gigaspace.write(data);

		final IntegratedProcessingUnitContainer ipuc = createContainer("classpath:/retries/META-INF/spring/pu.xml");

		try {
			final ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			waitForInstanceToReachStatus(usm, USMState.ERROR);

		} finally {
			ipuc.close();
		}

	}

	@Ignore
	@Test
	public void testDebug() throws IOException, InterruptedException {
		if (ServiceUtils.isWindows()) {
			// The debug feature only works for linux
			return;
		}
		final BeanLevelProperties blp = new BeanLevelProperties();
		final Properties contextProperties = new Properties();
		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_ALL, Boolean.TRUE.toString());
		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, DebugModes.INSTEAD.toString());

		blp.setContextProperties(contextProperties);

		deleteLockFile();
		final IntegratedProcessingUnitContainer ipuc =
				createContainer("classpath:/debug/META-INF/spring/pu.xml", blp);

		try {
			final ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			final USMState stateAtBreakpoint = getUsmState(usm);
			Assert.assertNotNull(stateAtBreakpoint);
			Assert.assertEquals(USMState.INITIALIZING, stateAtBreakpoint);

			final File lockFile = waitForDebugLockFile();
			System.out.println("Deleting lock file: " + lockFile.getAbsolutePath());
			FileUtils.deleteQuietly(lockFile);

			waitForInstanceToReachStatus(usm, USMState.RUNNING);

		} finally {
			ipuc.close();
		}

	}

	private void deleteLockFile() {
		final File lockFile = new File(DEBUG_LOCK_FILE_PATH);
		if (lockFile.exists()) {
			FileUtils.deleteQuietly(lockFile);
		}
	}

	private File waitForDebugLockFile() {

		final long endTime = System.currentTimeMillis() + 20000;
		while (System.currentTimeMillis() < endTime) {
			final File lockFile = new File(DEBUG_LOCK_FILE_PATH);
			if (lockFile.exists()) {
				return lockFile;
			}
		}
		Assert.fail("Expected debug lock file: " + DEBUG_LOCK_FILE_PATH + " was not created");
		return null;

	}

	private void waitForInstanceToReachStatus(final UniversalServiceManagerBean usm, final USMState targetState)
			throws InterruptedException {
		waitForInstanceToReachStatus(usm, targetState, 20000);
	}

	private void waitForInstanceToReachStatus(final UniversalServiceManagerBean usm, final USMState targetState,
			final long timeoutMillis)
			throws InterruptedException {
		final long start = System.currentTimeMillis();
		final long end = start + timeoutMillis;

		USMState currentState = null;
		while (System.currentTimeMillis() < end) {
			currentState = getUsmState(usm);
			if (currentState != null) {
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

	private USMState getUsmState(final UniversalServiceManagerBean usm) {
		final ServiceMonitors[] monitors = usm.getServicesMonitors();
		final Object state = monitors[0].getMonitors().get("USM_State");
		if (state != null) {
			final USMState stateEnum = USMState.values()[(Integer) state];
			return stateEnum;
		} else {
			return null;
		}

	}

	private ServiceInstanceAttemptData createServiceInstanceAttempDataTemplate() {
		final ServiceInstanceAttemptData data = new ServiceInstanceAttemptData();
		data.setApplicationName("default");
		data.setServiceName("groovyError");
		data.setGscPid(new Sigar().getPid());
		data.setInstanceId(1);
		return data;
	}

	@Ignore
	@Test
	public void testRecoveryAfterRetry() throws IOException, InterruptedException {
		final ServiceInstanceAttemptData data = createServiceInstanceAttempDataTemplate();
		data.setCurrentAttemptNumber(2);
		gigaspace.write(data);

		final IntegratedProcessingUnitContainer ipuc =
				createContainer("classpath:/retries-recovery/META-INF/spring/pu.xml");

		try {
			final ApplicationContext ctx = ipuc.getApplicationContext();
			final UniversalServiceManagerBean usm = ctx.getBean(UniversalServiceManagerBean.class);
			Assert.assertNotNull(usm);

			waitForInstanceToReachStatus(usm, USMState.RUNNING);

		} finally {
			ipuc.close();
		}

	}

	private IntegratedProcessingUnitContainer createContainer(final String classpath) throws IOException {
		return createContainer(classpath, null);
	}

	private IntegratedProcessingUnitContainer createContainer(final String classpath,
			final BeanLevelProperties beanLevelProperties) throws IOException {
		final IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
		// provide cluster information for the specific PU instance
		final ClusterInfo clusterInfo = new ClusterInfo();
		// clusterInfo.setSchema("partitioned-sync2backup");
		clusterInfo.setNumberOfInstances(1);
		clusterInfo.setInstanceId(1);
		clusterInfo.setName("default.groovyError");
		provider.setClusterInfo(clusterInfo);

		// set the config location (override the default one - classpath:/META-INF/spring/pu.xml)
		provider.addConfigLocation(classpath);
		if (beanLevelProperties != null) {
			provider.setBeanLevelProperties(beanLevelProperties);
		}

		// Build the Spring application context and "start" it
		final ProcessingUnitContainer container = provider.createContainer();
		return (IntegratedProcessingUnitContainer) container;

	}

	@AfterClass
	public static void afterClass() throws Exception {
		admin.close();

		urlSpaceConfigurer.destroy();

		System.out.println("Closed down embedded space");
	}

	@After
	public void after() throws SigarException {
		final Sigar sigar = new Sigar();
		final long[] allPids = sigar.getProcList();
		final long myPid = sigar.getPid();

		final Set<Long> childPids = new HashSet<Long>();
		for (final long pid : allPids) {
			try {
				final ProcState state = sigar.getProcState(pid);
				final long ppid = state.getPpid();

				if (ppid == myPid) {
					System.out.println("Found a leaking process: " + pid);
					childPids.add(pid);
				}
			} catch (final SigarException e) {
				// probably means that the process belongs to another user to may not have permissions
				// to read its data. Should be safe to ignore
			}
		}

		if (!childPids.isEmpty()) {
			System.out.println("Warning: found leaked processes after test finished: " + childPids);
			final DefaultProcessKiller killer = new DefaultProcessKiller();
			for (final Long pid : childPids) {
				final ProcExe procExe = sigar.getProcExe(pid);
				final String[] procArgs = sigar.getProcArgs(pid);
				System.out.println("Killing process: " + pid + ". Name: " + procExe.getName() + ", Args: "
						+ Arrays.toString(procArgs) + "Directory: " + procExe.getCwd());
				try {
					killer.killProcess(pid);
				} catch (final USMException e) {
					System.out.println("Failed to kill process: " + pid);
				}

			}

			Assert.fail("Leaked processes found after test.");
		}

	}
}
