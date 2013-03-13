package org.cloudifysource.shell.installer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ManagementRedeployerTest</code> contains tests for the class
 * <code>{@link ManagementRedeployer}</code>.
 *
 * @generatedBy CodePro at 3/12/13 4:11 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ManagementRedeployerTest {

	private File cloudifyHomeMockDir;
	private File persistenceMockDir;



	@Before
	public void setup() throws IOException {

		cloudifyHomeMockDir = File.createTempFile("ManagerRedeployTestHome", "tst");
		FileUtils.deleteQuietly(cloudifyHomeMockDir);
		cloudifyHomeMockDir.mkdirs();

		persistenceMockDir = File.createTempFile("ManagerRedeployTestDeploy", "tst");
		FileUtils.deleteQuietly(persistenceMockDir);
		persistenceMockDir.mkdirs();

		final File deployMockDir = new File(persistenceMockDir, "deploy");
		deployMockDir.mkdirs();

		File srcWarDir = new File("src/test/resources/sampleWar");
		File srcWarDir2 = new File("src/test/resources/sampleWar2");

		FileUtils.copyDirectory(srcWarDir, new File(deployMockDir, CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME));
		FileUtils.copyDirectory(srcWarDir, new File(deployMockDir, CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME));

		final File mockRestFile = new File(cloudifyHomeMockDir + "/tools/rest/rest.war");
		mockRestFile.mkdirs();
		mockRestFile.delete();

		final File mockWebuiFile = new File(cloudifyHomeMockDir + "/tools/gs-webui/gs-webui-9.5.0-SNAPSHOT.war");
		mockWebuiFile.mkdirs();
		mockWebuiFile.delete();
		ZipUtils.zip(srcWarDir2, mockRestFile);
		ZipUtils.zip(srcWarDir2, mockWebuiFile);

	}

	@After
	public void cleanup() {
		if (cloudifyHomeMockDir != null) {
			FileUtils.deleteQuietly(cloudifyHomeMockDir);
		}

		if (persistenceMockDir != null) {
			FileUtils.deleteQuietly(persistenceMockDir);
		}
	}

	/**
	 * Run the void run(String,String) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 3/12/13 4:11 PM
	 */
	@Test
	public void testRun()
			throws Exception {

		final File restIndex2 =
				new File(persistenceMockDir + "/deploy/" + CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME
						+ "/index2.html");
		final File webuiIndex2 =
				new File(persistenceMockDir + "/deploy/" + CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME
						+ "/index2.html");
		Assert.assertTrue(!restIndex2.exists());
		Assert.assertTrue(!webuiIndex2.exists());
		ManagementRedeployer fixture = new ManagementRedeployer();
		fixture.run(this.persistenceMockDir.getAbsolutePath(), this.cloudifyHomeMockDir.getAbsolutePath());

		Assert.assertTrue(restIndex2.exists());
		Assert.assertTrue(webuiIndex2.exists());

		Assert.assertTrue(fixture.isRestRedeployed());
		Assert.assertTrue(fixture.isWebuiRedeployed());

	}

	@Test
	public void testRunNoPersistence()
			throws Exception {

		ManagementRedeployer fixture = new ManagementRedeployer();
		fixture.run(null, this.cloudifyHomeMockDir.getAbsolutePath());

		Assert.assertTrue(!fixture.isRestRedeployed());
		Assert.assertTrue(!fixture.isWebuiRedeployed());
	}

	@Test
	public void testRunPersistenceDirectoryMissing()
			throws Exception {

		final File tempFile = File.createTempFile("tempfile", "temp");
		tempFile.delete();
		ManagementRedeployer fixture = new ManagementRedeployer();
		fixture.run(tempFile.getAbsolutePath(), this.cloudifyHomeMockDir.getAbsolutePath());

		Assert.assertTrue(!fixture.isRestRedeployed());
		Assert.assertTrue(!fixture.isWebuiRedeployed());
	}

}