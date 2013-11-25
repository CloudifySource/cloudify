package org.cloudifysource.dsl.internal;

import groovy.util.ConfigObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * The class <code>GroovyPropertiesFileReaderTest</code> contains tests for the class
 * <code>{@link GroovyPropertiesFileReader}</code>.
 * 
 * @generatedBy CodePro at 11/20/13 10:24 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class GroovyPropertiesFileReaderTest {
	/**
	 * Run the Map<Object, Object> readPropertiesFile(File) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/20/13 10:24 PM
	 */
	@Test
	public void testReadPropertiesFile_1()
			throws Exception {
		GroovyPropertiesFileReader fixture = new GroovyPropertiesFileReader();
		File propertiesFile = null;

		Map<Object, Object> result = fixture.readPropertiesFile(propertiesFile);

		// add additional test code here
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	/**
	 * Run the Map<Object, Object> readPropertiesFile(File) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11/20/13 10:24 PM
	 */
	@Test(expected = java.io.IOException.class)
	public void testReadPropertiesFile_2()
			throws Exception {
		GroovyPropertiesFileReader fixture = new GroovyPropertiesFileReader();
		File propertiesFile = new File("");

		Map<Object, Object> result = fixture.readPropertiesFile(propertiesFile);

		// add additional test code here
		assertNotNull(result);
	}

	@Test
	public void testReadPropertiesFileSample()
			throws Exception {
		GroovyPropertiesFileReader fixture = new GroovyPropertiesFileReader();
		File propertiesFile =
				new File(
						"src/test/resources/org/cloudifysource/dsl/internal/GroovyPropertiesFileReaderTest/some-service.properties");

		Map<Object, Object> result = fixture.readPropertiesFile(propertiesFile);

		// add additional test code here
		assertNotNull(result);
		assertTrue(result.containsKey("foo"));
		assertTrue(result.containsKey("a"));
		assertTrue(result.containsKey("b"));
	}

	/********
	 * Test for perm gen issues in properties file reader. 
	 * This test is disabled as it takes a long time to run. It	 is left here in case
	 * we need to recreate this issue in the future.
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void testPermGen() throws IOException {
		GroovyPropertiesFileReader fixture = new GroovyPropertiesFileReader();
		File propertiesFile =
				new File(
						"src/test/resources/org/cloudifysource/dsl/internal/GroovyPropertiesFileReaderTest/some-service.properties");

		for (int i = 0; i < 10000; ++i) {
			fixture.readPropertiesFile(propertiesFile);
			if (i % 10 == 0) {
				System.out.println(i);
			}
		}

	}

	@Test
	public void testReturnObjectIsConfigObject() throws IOException {
		GroovyPropertiesFileReader fixture = new GroovyPropertiesFileReader();
		LinkedHashMap<Object, Object> retval = fixture.readPropertiesFile(null);
		Assert.assertNotNull(retval);
		Assert.assertTrue(retval instanceof ConfigObject);
		
		LinkedHashMap<Object, Object> retval2 = fixture.readPropertiesScript(null);
		Assert.assertNotNull(retval2);
		Assert.assertTrue(retval2 instanceof ConfigObject);
		
		
	}
	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11/20/13 10:24 PM
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
	 * @generatedBy CodePro at 11/20/13 10:24 PM
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
	 * @generatedBy CodePro at 11/20/13 10:24 PM
	 */
	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(GroovyPropertiesFileReaderTest.class);
	}
}