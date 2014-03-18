package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.junit.*;

import com.j_spaces.kernel.PlatformVersion;

/**
 * The class <code>APIVersionResolverTest</code> contains tests for the class <code>{@link APIVersionResolver}</code>.
 * 
 * @generatedBy CodePro at 3/18/14 1:54 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class APIVersionResolverTest {
	/**
	 * Run the String resolveAPIVersion() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 3/18/14 1:54 PM
	 */
	@Test
	public void testResolveAPIVersionFromSysProp()
			throws Exception {
		APIVersionResolver fixture = new APIVersionResolver();

		final String sysPropBefore = System.getProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);
		String value = "10.0.0";
		System.setProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION, value);

		String result = fixture.resolveAPIVersion();
		try {
			Assert.assertEquals(value, result);
		} finally {
			if (sysPropBefore == null) {
				System.clearProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);
			} else {
				System.setProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION, sysPropBefore);
			}

		}

	}

	/**
	 * Run the String resolveAPIVersion() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 3/18/14 1:54 PM
	 */
	@Test
	public void testResolveAPIVersionByPlatformVersion()
			throws Exception {
		APIVersionResolver fixture = new APIVersionResolver();

		final String sysPropBefore = System.getProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);
		System.clearProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);

		String result = fixture.resolveAPIVersion();
		try {
			Assert.assertEquals(PlatformVersion.getVersion(), result);
		} finally {
			if (sysPropBefore == null) {
				System.clearProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);
			} else {
				System.setProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION, sysPropBefore);
			}

		}
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 3/18/14 1:54 PM
	 */
	@Before
	public void setUp()
			throws Exception {
		// add additional set up code here
	}

	/**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception
	 *             if the clean-up fails for some reason
	 * 
	 * @generatedBy CodePro at 3/18/14 1:54 PM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}

}