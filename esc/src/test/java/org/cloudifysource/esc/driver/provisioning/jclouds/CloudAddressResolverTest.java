package org.cloudifysource.esc.driver.provisioning.jclouds;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CloudAddressResolverTest</code> contains tests for the class
 * <code>{@link CloudAddressResolver}</code>.
 *
 * @generatedBy CodePro at 5/12/13 12:08 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class CloudAddressResolverTest {
	/**
	 * Run the String getAddress(Set<String>,Set<String>,SubnetInfo,Pattern) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 5/12/13 12:08 PM
	 */
	@Test
	public void testGetAddress_1()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet(Arrays.asList("10.4.12.1"));
		final Set<String> backupAddresses = new HashSet();
		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils("10.4.12.0/22").getInfo();
		final Pattern regex = null;

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals("10.4.12.1", result);
	}


	@Test
	public void testGetAddress_2()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet();
		final Set<String> backupAddresses = new HashSet();
		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils("10.4.12.0/22").getInfo();
		final Pattern regex = null;

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals(null, result);
	}

	@Test
	public void testGetAddress_3()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet(Arrays.asList("10.4.12.1"));
		final Set<String> backupAddresses = new HashSet();
		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = null;
		final Pattern regex = Pattern.compile("10.4.12.*");

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals("10.4.12.1", result);
	}

	@Test
	public void testGetAddress_4()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet(Arrays.asList("10.4.12.1", "10.4.13.12"));
		final Set<String> backupAddresses = new HashSet();
		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = null;
		final Pattern regex = Pattern.compile("10.4.13.*");

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals("10.4.13.12", result);
	}

	@Test
	public void testGetAddress_5()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet();
		final Set<String> backupAddresses = new HashSet(Arrays.asList("10.4.12.1", "10.4.13.12"));

		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = null;
		final Pattern regex = Pattern.compile("10.4.13.*");

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals("10.4.13.12", result);
	}

	@Test
	public void testGetAddress_6()
			throws Exception {
		final CloudAddressResolver fixture = new CloudAddressResolver();
		final Set<String> addresses = new HashSet();
		final Set<String> backupAddresses = new HashSet(Arrays.asList("10.4.12.1", "10.4.15.12"));

		final org.apache.commons.net.util.SubnetUtils.SubnetInfo subnetInfo = null;
		final Pattern regex = Pattern.compile("10.4.13.*");

		final String result = fixture.getAddress(addresses, backupAddresses, subnetInfo, regex);

		// add additional test code here
		assertEquals(null, result);
	}

	/**
	 * Perform pre-test initialization.
	 *
	 * @throws Exception
	 *             if the initialization fails for some reason
	 *
	 * @generatedBy CodePro at 5/12/13 12:08 PM
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
	 * @generatedBy CodePro at 5/12/13 12:08 PM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}

	/**
	 * Launch the test.
	 *
	 * @param args
	 *            the command line arguments
	 *
	 * @generatedBy CodePro at 5/12/13 12:08 PM
	 */
	public static void main(final String[] args) {
		new org.junit.runner.JUnitCore().run(CloudAddressResolverTest.class);
	}
}