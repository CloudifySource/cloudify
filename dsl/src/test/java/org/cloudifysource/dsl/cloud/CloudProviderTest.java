package org.cloudifysource.dsl.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.cloudifysource.dsl.cloud.CloudProvider;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * The class <code>CloudProviderTest</code> contains tests for the class <code>{@link CloudProvider}</code>.
 * 
 * @generatedBy CodePro at 6/27/12 1:00 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class CloudProviderTest {

	/**
	 * Run the String getCloudifyUrl() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 6/27/12 1:00 PM
	 */
	@Test
	public void testGetCloudifyUrl_1()
			throws Exception {
		final CloudProvider fixture = new CloudProvider();
		fixture.setMachineNamePrefix("");
		fixture.setReservedMemoryCapacityPerMachineInMB(1);
		fixture.setProvider("");
		fixture.setManagementOnlyFiles(new ArrayList());

		fixture.setCloudifyUrl("");
		fixture.setNumberOfManagementMachines(1);
		fixture.setManagementGroup("");
		fixture.setCloudifyOverridesUrl("");
		fixture.setSshLoggingLevel("");

		final String result = fixture.getCloudifyUrl();

		// add additional test code here
		assertEquals("", result);
		// unverified
	}

	/**
	 * Run the String getCloudifyUrl() method test with the default value.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 6/27/12 1:00 PM
	 */
	@Test
	public void testGetCloudifyUrl_2()
			throws Exception {
		final CloudProvider fixture = new CloudProvider();
		fixture.setMachineNamePrefix("");
		fixture.setReservedMemoryCapacityPerMachineInMB(1);
		fixture.setProvider("");
		fixture.setManagementOnlyFiles(new ArrayList());
		fixture.setNumberOfManagementMachines(1);
		fixture.setManagementGroup("");
		fixture.setCloudifyOverridesUrl("");
		fixture.setSshLoggingLevel("");

		final String result = fixture.getCloudifyUrl();

		// add additional test code here
		assertTrue("Default cloudify url should point to the cloudifysource repo",
				result.startsWith("http://repository.cloudifysource.org"));
		// unverified
	}


	/**
	 * Run the String getCloudifyUrl() method and check the edition.
	 * 
	 * @throws Exception
	 * 
	 */
	@Test
	public void testEditionInCloudifyUrl()
			throws Exception {
		final CloudProvider fixture = new CloudProvider();
		fixture.setMachineNamePrefix("");
		fixture.setReservedMemoryCapacityPerMachineInMB(1);
		fixture.setProvider("");
		fixture.setManagementOnlyFiles(new ArrayList());
		fixture.setNumberOfManagementMachines(1);
		fixture.setManagementGroup("");
		fixture.setCloudifyOverridesUrl("");
		fixture.setSshLoggingLevel("");

		final String result = fixture.getCloudifyUrl();
		String cloudifyEdition = PlatformVersion.getEdition();
		assertTrue("Can not recognize cloudify edition.",
				cloudifyEdition.equals(CloudifyConstants.CLOUDIFY_BIGDATA_EDITION) || cloudifyEdition.equals(CloudifyConstants.CLOUDIFY_EDITION));
		assertTrue("cloudify url not containing any of he known editions", 
				result.contains("/gigaspaces-xap-premium-") || result.contains("/gigaspaces-cloudify-"));
		assertTrue("cloudify url not containing valid product key", 
				result.contains("com/gigaspaces/xap") || result.contains("org/cloudifysource"));
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 6/27/12 1:00 PM
	 */
	@Before
	public void setUp()
			throws Exception {
		// add additional set up code here
	}

	/**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception if the clean-up fails for some reason
	 * 
	 * @generatedBy CodePro at 6/27/12 1:00 PM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}