package org.cloudifysource.esc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.zone.config.ExactZonesConfig;

/**
 * The class <code>UtilsTest</code> contains tests for the class <code>{@link Utils}</code>.
 * 
 * @generatedBy CodePro at 11/14/13 12:40 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class UtilsTest {
	/**
	 * Run the void threadSleep(long) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/14/13 12:40 PM
	 */
	@Test
	public void testCreateInstallationDetailsWithMachineDetailsEnvironment()
			throws Exception {
		final MachineDetails md = new MachineDetails();
		md.getEnvironment().put("MARKER", "MARKER_VALUE");

		final Cloud cloud = new Cloud();
		cloud.getProvider().setManagementOnlyFiles(new ArrayList<String>());
		final ComputeTemplate template = new ComputeTemplate();
		final ExactZonesConfig zones = new ExactZonesConfig();
		zones.setZones(new HashSet<String>(Arrays.asList("Zone1")));

		final File cloudFile = new File("no-cloud.groovy");
		final GSAReservationId reservationId = new GSAReservationId("123456");
		final String templateName = "SMALL_LINUX";

		final InstallationDetails result = Utils.createInstallationDetails(md, cloud, template, zones,
				"localhost:4177", null, false, cloudFile, reservationId, templateName,
				null, null, null, false);

		Assert.assertNotNull(result.getExtraRemoteEnvironmentVariables());
		Assert.assertTrue("Expected to find environment variable", result.getExtraRemoteEnvironmentVariables()
				.containsKey("MARKER"));
		Assert.assertEquals("MARKER_VALUE", result.getExtraRemoteEnvironmentVariables().get("MARKER"));

		// add additional test code here
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11/14/13 12:40 PM
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
	 * @generatedBy CodePro at 11/14/13 12:40 PM
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
	 * @generatedBy CodePro at 11/14/13 12:40 PM
	 */
	public static void main(final String[] args) {
		new org.junit.runner.JUnitCore().run(UtilsTest.class);
	}
}