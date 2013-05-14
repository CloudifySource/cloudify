import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.cloudifysource.dsl.context.kvstorage.spaceentries.ApplicationCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.GlobalCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.InstanceCloudifyAttribute;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.ServiceCloudifyAttribute;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.j_spaces.core.IJSpace;

/**
 * @author Dan Kilman
 */
public class ManagementSpacePUTest {

	private static final String SPRING_ACTIVE_PROFILES_PROP = "spring.profiles.active";
	private static final String MANAGEMENT_SPACE_PU_XML = "/META-INF/spring/pu.xml";

	private final GlobalCloudifyAttribute globalCloudifyAttribute = new GlobalCloudifyAttribute("global", "global");
	private final ApplicationCloudifyAttribute applicationCloudifyAttribute = new ApplicationCloudifyAttribute("app",
			"app", "app");
	private final ServiceCloudifyAttribute serviceCloudifyAttribute = new ServiceCloudifyAttribute("app", "service",
			"service", "service");
	private final InstanceCloudifyAttribute instanceCloudifyAttribute = new InstanceCloudifyAttribute("app", "service",
			1, "instance", "instance");
	private final CloudConfigurationHolder cloudConfigurationHolder = new CloudConfigurationHolder("cloudConfig",
			"cloudConfigPath");

	/*****
	 * Set the server hostname explicitly (equivalent to NIC_ADDR environment variable) to prevent travis build failing
	 * on ipv6 issue.
	 *
	 * @throws UnknownHostException .
	 */
	@Before
	public void before() throws UnknownHostException {
		//final String ip = java.net.InetAddress.getLocalHost().getHostAddress();
		// Adding localhost nic address to try and get past travis ipv6 issue
		final String ip = "127.0.0.1";
		System.setProperty("java.rmi.server.hostname", ip);
	}

	@Test
	public void testNonPersistentSpace() throws IOException {
		testManagementSpaceImpl(false);
	}

	@Test
	public void testPersistentSpace() throws IOException {
		testManagementSpaceImpl(true);
	}

	@After
	public void after() {
		System.setProperty(SPRING_ACTIVE_PROFILES_PROP, "");
	}

	private void testManagementSpaceImpl(boolean persistent) throws IOException {

		String activeProfiles = persistent ? CloudifyConstants.PERSISTENCE_PROFILE_PERSISTENT :
				CloudifyConstants.PERSISTENCE_PROFILE_TRANSIENT;

		System.setProperty(SPRING_ACTIVE_PROFILES_PROP, activeProfiles);

		final ClassPathXmlApplicationContext context = createSpringContextContext();

		context.refresh();
		try {
			IJSpace space = context.getBean(IJSpace.class);
			GigaSpace gigaSpace = new GigaSpaceConfigurer(space).create();
			doWrites(gigaSpace);
		} finally {
			context.close();
		}

		context.refresh();
		try {
			IJSpace space = context.getBean(IJSpace.class);
			GigaSpace gigaSpace = new GigaSpaceConfigurer(space).create();
			doReadsAndAsserts(gigaSpace, persistent);
		} finally {
			context.close();
		}
	}

	private ClassPathXmlApplicationContext createSpringContextContext() throws IOException {
		File tempStorageFile = File.createTempFile("management-space-test", "");
		final boolean refreshNow = false;
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext
				(new String[] { MANAGEMENT_SPACE_PU_XML }, refreshNow);
		PropertyPlaceholderConfigurer propertyConfigurer = new PropertyPlaceholderConfigurer();
		Properties properties = new Properties();
		properties.setProperty("space.name", "management-space");
		properties.setProperty("space.storage.path", tempStorageFile.getAbsolutePath().replace('\\', '/'));
		propertyConfigurer.setProperties(properties);
		context.addBeanFactoryPostProcessor(propertyConfigurer);
		return context;
	}

	private void doWrites(GigaSpace gigaSpace) {
		gigaSpace.write(globalCloudifyAttribute);
		gigaSpace.write(applicationCloudifyAttribute);
		gigaSpace.write(serviceCloudifyAttribute);
		gigaSpace.write(instanceCloudifyAttribute);
		gigaSpace.write(cloudConfigurationHolder);
	}

	private void doReadsAndAsserts(GigaSpace gigaSpace, boolean persistent) {

		GlobalCloudifyAttribute global = gigaSpace.read(new GlobalCloudifyAttribute());
		ApplicationCloudifyAttribute application = gigaSpace.read(new ApplicationCloudifyAttribute());
		ServiceCloudifyAttribute service = gigaSpace.read(new ServiceCloudifyAttribute());
		InstanceCloudifyAttribute instance = gigaSpace.read(new InstanceCloudifyAttribute());
		CloudConfigurationHolder cloudConfig = gigaSpace.read(new CloudConfigurationHolder());

		if (persistent) {
			Assert.assertNotNull(global);
			Assert.assertNotNull(application);
			Assert.assertNotNull(service);
			Assert.assertNotNull(instance);
			Assert.assertNotNull(cloudConfig);

			Assert.assertEquals(globalCloudifyAttribute.getApplicationName(), global.getApplicationName());
			Assert.assertEquals(globalCloudifyAttribute.getKey(), global.getKey());
			Assert.assertEquals(globalCloudifyAttribute.getUid(), global.getUid());
			Assert.assertEquals(globalCloudifyAttribute.getValue(), global.getValue());

			Assert.assertEquals(applicationCloudifyAttribute.getApplicationName(), application.getApplicationName());
			Assert.assertEquals(applicationCloudifyAttribute.getKey(), application.getKey());
			Assert.assertEquals(applicationCloudifyAttribute.getUid(), application.getUid());
			Assert.assertEquals(applicationCloudifyAttribute.getValue(), application.getValue());

			Assert.assertEquals(serviceCloudifyAttribute.getApplicationName(), service.getApplicationName());
			Assert.assertEquals(serviceCloudifyAttribute.getKey(), service.getKey());
			Assert.assertEquals(serviceCloudifyAttribute.getUid(), service.getUid());
			Assert.assertEquals(serviceCloudifyAttribute.getValue(), service.getValue());
			Assert.assertEquals(serviceCloudifyAttribute.getServiceName(), service.getServiceName());

			Assert.assertEquals(instanceCloudifyAttribute.getApplicationName(), instance.getApplicationName());
			Assert.assertEquals(instanceCloudifyAttribute.getKey(), instance.getKey());
			Assert.assertEquals(instanceCloudifyAttribute.getUid(), instance.getUid());
			Assert.assertEquals(instanceCloudifyAttribute.getValue(), instance.getValue());
			Assert.assertEquals(instanceCloudifyAttribute.getServiceName(), instance.getServiceName());
			Assert.assertEquals(instanceCloudifyAttribute.getInstanceId(), instance.getInstanceId());

			Assert.assertEquals(cloudConfigurationHolder.getId(), cloudConfig.getId());
			Assert.assertEquals(cloudConfigurationHolder.getCloudConfigurationFilePath(),
					cloudConfig.getCloudConfigurationFilePath());
			Assert.assertEquals(cloudConfigurationHolder.getCloudConfiguration(), cloudConfig.getCloudConfiguration());
		} else {
			Assert.assertNull(global);
			Assert.assertNull(application);
			Assert.assertNull(service);
			Assert.assertNull(instance);
			Assert.assertNull(cloudConfig);
		}
	}

}
