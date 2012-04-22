package org.cloudifysource.dsl.cloud;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CloudUserTest</code> contains tests for the class <code>{@link CloudUser}</code>.
 * 
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class CloudUserTest {

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testValidateKeyFileDefaultValue_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_2()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("ENTER_USER");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_3()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("ENTER_KEY");
		fixture.setKeyFile("");

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_4()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("ENTER_KEY_FILE_NAME");

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 */
	@Test
	public void testValidateKeyFileDefaultValue_5()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser(null);
		fixture.setApiKey(null);
		fixture.setKeyFile(null);

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 */
	@Test
	public void testValidateKeyFileDefaultValue_6()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		fixture.validateDefaultValues();

		// add additional test code here
		// unverified
	}
	
	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
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
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}