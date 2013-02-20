// CHECKSTYLE:OFF
package org.cloudifysource.shell.commands;

import java.io.File;

import org.cloudifysource.shell.ShellUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ValidateServiceTest</code> contains tests for the class <code>{@link ValidateService}</code>.
 * 
 * @generatedBy CodePro at 8/10/12 12:14 AM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ValidateServiceTest {

	/**
	 * Run the Object doExecute() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/10/12 12:14 AM
	 */
	@Test(expected = CLIStatusException.class)
	public void testMissingFile()
			throws Exception {

		final ValidateService fixture = new ValidateService();
		fixture.setServiceFile(new File("AAAA"));
		fixture.messages = ShellUtils.getMessageBundle();

		fixture.doExecute();
	}

	/**
	 * Run the Object doExecute() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/10/12 12:14 AM
	 */
	@Test()
	public void testValidService()
			throws Exception {
		final ValidateService fixture = new ValidateService();
		fixture.setServiceFile(new File("src/test/resources/serviceValidation/groovy-service"));
		fixture.messages = ShellUtils.getMessageBundle();

		final Object result = fixture.doExecute();

		Assert.assertTrue(((String) result).contains("success"));
	}

	/**
	 * Run the File getServiceFile() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/10/12 12:14 AM
	 */
	@Test(expected = CLIStatusException.class)
	public void testInvalidService()
			throws Exception {

		final ValidateService fixture = new ValidateService();
		fixture.setServiceFile(new File("src/test/resources/serviceValidation/failed-groovy-service"));
		fixture.messages = ShellUtils.getMessageBundle();

		fixture.doExecute();

	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 8/10/12 12:14 AM
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
	 * @generatedBy CodePro at 8/10/12 12:14 AM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}