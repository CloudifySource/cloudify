// CHECKSTYLE:OFF
package org.cloudifysource.shell.commands;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.shell.ShellUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ValidateApplicationTest</code> contains tests for the class <code>{@link ValidateApplication}</code>.
 * 
 * @generatedBy CodePro at 8/9/12 11:48 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ValidateApplicationTest {

	/**
	 * Run the Object doExecute() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/9/12 11:48 PM
	 */
	@Test(expected = CLIStatusException.class)
	public void testMissingFile()
			throws Exception {
		final ValidateApplication fixture = new ValidateApplication();
		fixture.setApplicationFile(new File(""));
		fixture.messages = ShellUtils.getMessageBundle();

		fixture.doExecute();

	}

	/**
	 * Run the Object doExecute() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/9/12 11:48 PM
	 */
	@Test
	public void testValidApp()
			throws Exception {
		final ValidateApplication fixture = new ValidateApplication();
		fixture.setApplicationFile(new File("src/test/resources/applicationValidation/groovy-application"));
		fixture.messages = ShellUtils.getMessageBundle();

		final Object result = fixture.doExecute();

		Assert.assertTrue(((String) result).contains("success"));
	}

	/**
	 * Run the File getApplicationFile() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 8/9/12 11:48 PM
	 */
	@Test(expected = CLIStatusException.class)
	public void testInvalidApp()
			throws Exception {
		final ValidateApplication fixture = new ValidateApplication();
		fixture.setApplicationFile(new File("src/test/resources/applicationValidation/failed-groovy-application"));
		fixture.messages = ShellUtils.getMessageBundle();

		fixture.doExecute();

		


	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 8/9/12 11:48 PM
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
	 * @generatedBy CodePro at 8/9/12 11:48 PM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}