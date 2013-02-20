package org.cloudifysource.dsl.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.validator.routines.UrlValidator;
import org.cloudifysource.dsl.internal.ServiceReader;
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
	
	
	private  static final String WRONG_PROVIDER_PATH = "testResources/testparsing/wrong-provider.groovy";
	private  static final String INVALID_URL_PATH = "testResources/testparsing/invalid-url.groovy";
	private  static final String INVALID_NUM_MGMT_MACHINES_PATH = 
			"testResources/testparsing/invalid-num-mgmt-machines.groovy";
	private  static final String INVALID_SSH_LOG_LEVEL_PATH = "testResources/testparsing/invalid-ssh-log-level.groovy";

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
	 * Run the String getCloudifyUrl() method and check it's validity. 
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
		assertTrue("Default cloudify url should point to the cloudifysource repo",
				result.startsWith("http://repository.cloudifysource.org"));
		
		String cloudifyEdition = PlatformVersion.getEdition();
		assertTrue("Can not recognize cloudify edition.",
				cloudifyEdition.equals(PlatformVersion.EDITION_XAP_PREMIUM) || cloudifyEdition.equals(PlatformVersion.EDITION_CLOUDIFY));
		assertTrue("cloudify url not containing any of the known editions", 
				result.contains("/gigaspaces-xap-premium-") || result.contains("/gigaspaces-cloudify-"));
		assertTrue("cloudify url not containing valid product key", 
				result.contains("com/gigaspaces/xap") || result.contains("org/cloudifysource"));
		assertTrue("Cloudify url was not formatted properly", !result.contains("%s"));
		validateUrlFormat(result);
		
	}

	private void validateUrlFormat(final String result) {
		String[] schema = {"http"};
		UrlValidator urlValidator = new UrlValidator(schema);
		assertTrue("Cloudify URL validation on url " + result + " failed", urlValidator.isValid(result));
	}


	@Test
	public void testProviderNameValidation() throws Exception {
		try {
			ServiceReader.readCloud(new File(WRONG_PROVIDER_PATH));
			// if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The provider name is invalid yet no error was thrown", false);
		} catch (final Throwable e) {
			assertTrue("The provider name is invalid yet no relevant error was thrown. Error was: " + e.getMessage(),
					e.getMessage().contains("Provider cannot be empty"));
		}
	}
	
	/*@Test
	public void testCloudifyUrlValidation() throws Exception {
		try {
			ServiceReader.readCloud(new File(INVALID_URL_PATH));
			// if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The Cloudify url is invalid yet no error was thrown", false);
		} catch (final Throwable e) {
			assertTrue("The Cloudify url is invalid yet no relevant error was thrown. Error was: " + e.getMessage(),
					e.getMessage().contains("Invalid cloudify url"));
		}
	}*/
	
	@Test
	public void testNumberOfManagementMachinesValidation() throws Exception {
		try {
			ServiceReader.readCloud(new File(INVALID_NUM_MGMT_MACHINES_PATH));
			// if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The number of management machines is invalid yet no error was thrown", false);
		} catch (final Throwable e) {
			assertTrue("The number of management machines is invalid yet no relevant error was thrown. Error was: " 
					+ e.getMessage(), e.getMessage().contains("Invalid numberOfManagementMachines"));
		}
	}
	
	@Test
	public void testSshLoggingLevelValidation() throws Exception {
		try {
			ServiceReader.readCloud(new File(INVALID_SSH_LOG_LEVEL_PATH));
			// if we got to the next line - the validation exception wasn't thrown.
			assertTrue("The ssh logging level is invalid yet no error was thrown", false);
		} catch (final Throwable e) {
			assertTrue("The ssh logging level is invalid yet no relevant error was thrown. Error was: "
					+ e.getMessage(), e.getMessage().contains("INFO, FINE, FINER, FINEST, WARNING, DEBUG"));
		}
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