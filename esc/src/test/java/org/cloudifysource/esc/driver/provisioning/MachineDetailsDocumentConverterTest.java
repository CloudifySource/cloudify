package org.cloudifysource.esc.driver.provisioning;

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;
import com.gigaspaces.document.SpaceDocument;

/**
 * The class <code>MachineDetailsDocumentConverterTest</code> contains tests for the class
 * <code>{@link MachineDetailsDocumentConverter}</code>.
 * 
 * @generatedBy CodePro at 11/24/13 5:46 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class MachineDetailsDocumentConverterTest {
	/**
	 * Run the SpaceDocument toDocument(MachineDetails) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test
	public void testToDocument_1()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		MachineDetails md = null;

		SpaceDocument result = fixture.toDocument(md);

		// add additional test code here
		assertEquals(null, result);
	}

	/**
	 * Run the SpaceDocument toDocument(MachineDetails) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test
	public void testToDocument_2()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		MachineDetails md = new MachineDetails();

		SpaceDocument result = fixture.toDocument(md);

		// add additional test code here
		assertNotNull(result);
		assertEquals(
				"SpaceDocument [typeName=org.cloudifysource.esc.driver.provisioning.PersistentMachineDetails, version=0, transient=false, properties=DocumentProperties {keyFile=null,machineId=null,agentRunning=false,privateAddress=null,remoteDirectory=null,locationId=null,remoteExecutionMode=SSH,remoteUsername=null,keyFileName=null,remotePassword=null,publicAddress=null,scriptLangeuage=LINUX_SHELL,installationDirectory=null,cloudifyInstalled=false,fileTransferMode=SFTP,environment={},cleanRemoteDirectoryOnStart=false,openFilesLimit=null}]",
				result.toString());
		assertEquals("org.cloudifysource.esc.driver.provisioning.PersistentMachineDetails", result.getTypeName());
		assertEquals(false, result.isTransient());
		assertEquals(0, result.getVersion());
	}

	/**
	 * Run the MachineDetails toMachineDetails(SpaceDocument) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test
	public void testToMachineDetails_1()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		SpaceDocument document = null;

		MachineDetails result = fixture.toMachineDetails(document);

		// add additional test code here
		assertEquals(null, result);
	}

	/**
	 * Run the MachineDetails toMachineDetails(SpaceDocument) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test(expected = java.lang.IllegalStateException.class)
	public void testToMachineDetails_2()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		SpaceDocument document = new SpaceDocument();

		MachineDetails result = fixture.toMachineDetails(document);

		// add additional test code here
		assertNotNull(result);
	}

	/**
	 * Run the MachineDetails toMachineDetails(SpaceDocument) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test(expected = java.lang.IllegalStateException.class)
	public void testToMachineDetails_3()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		SpaceDocument document = new SpaceDocument();

		MachineDetails result = fixture.toMachineDetails(document);

		// add additional test code here
		assertNotNull(result);
	}

	/**
	 * Run the MachineDetails toMachineDetails(SpaceDocument) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	@Test(expected = java.lang.IllegalStateException.class)
	public void testToMachineDetails_4()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		SpaceDocument document = new SpaceDocument();

		MachineDetails result = fixture.toMachineDetails(document);

		// add additional test code here
		assertNotNull(result);
	}

	@Test
	public void testConvertBackAndForth()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		MachineDetails md = new MachineDetails();
		md.setMachineId("12345");
		md.setLocationId("abcde");

		SpaceDocument document = fixture.toDocument(md);

		MachineDetails md2 = fixture.toMachineDetails(document);

		Assert.assertEquals(md.getMachineId(), md2.getMachineId());
		Assert.assertEquals(md.getLocationId(), md2.getLocationId());

	}

	@Test
	public void testConvertBackAndForthWithFile()
			throws Exception {
		MachineDetailsDocumentConverter fixture = new MachineDetailsDocumentConverter();
		MachineDetails md = new MachineDetails();
		md.setMachineId("12345");
		md.setLocationId("abcde");
		final File tmpFile = new File("someTempFile");
		final String tempFilePath = tmpFile.getAbsolutePath(); 
		md.setKeyFile(tmpFile);
		
		SpaceDocument document = fixture.toDocument(md);

		Assert.assertEquals(PersistentMachineDetails.class.getName(), document.getTypeName());
		Assert.assertTrue(document.containsProperty("keyFile"));
		Assert.assertTrue(document.containsProperty("keyFileName"));
		Assert.assertEquals(tempFilePath, document.getProperty("keyFileName"));
		Assert.assertNull(document.getProperty("keyFile"));
		
		
		MachineDetails md2 = fixture.toMachineDetails(document);

		Assert.assertEquals(md.getMachineId(), md2.getMachineId());
		Assert.assertEquals(md.getLocationId(), md2.getLocationId());
		Assert.assertNotNull(md2.getKeyFile());
		Assert.assertTrue(md2 instanceof MachineDetails);
		Assert.assertFalse(md2 instanceof PersistentMachineDetails);
		
		final String filePath2 = md2.getKeyFile().getAbsolutePath();
		Assert.assertEquals(tempFilePath, filePath2);

	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11/24/13 5:46 PM
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
	 * @generatedBy CodePro at 11/24/13 5:46 PM
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
	 * @generatedBy CodePro at 11/24/13 5:46 PM
	 */
	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(MachineDetailsDocumentConverterTest.class);
	}
}