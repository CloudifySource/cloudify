package org.cloudifysource.dsl.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CloudUserTest</code> contains tests for the class <code>{@link CloudUser}</code>.
 * 
 * @generatedBy CodePro at 4/5/12 10:11 AM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class CloudUserTest {

	/**
	 * Run the String getApiKey() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testGetApiKey_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		final String result = fixture.getApiKey();

		// add additional test code here
		assertEquals("", result);
		// unverified
	}

	/**
	 * Run the String getKeyFile() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testGetKeyFile_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		final String result = fixture.getKeyFile();

		// add additional test code here
		assertEquals("", result);
		// unverified
	}

	/**
	 * Run the String getUser() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testGetUser_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		final String result = fixture.getUser();

		// add additional test code here
		assertEquals("", result);
		// unverified
	}

	/**
	 * Run the void setApiKey(String) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testSetApiKey_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");
		final String apiKey = "";

		fixture.setApiKey(apiKey);

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void setKeyFile(String) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testSetKeyFile_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");
		final String keyFile = "";

		fixture.setKeyFile(keyFile);

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void setUser(String) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testSetUser_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");
		final String user = "";

		fixture.setUser(user);

		// add additional test code here
		// unverified
	}

	/**
	 * Run the String toString() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testToString_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		final String result = fixture.toString();

		// add additional test code here
		assertEquals("CloudUser [user=, keyFile=]", result);
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test
	public void testValidateKeyFileDefaultValue_1()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		fixture.validateKeyFileDefaultValue();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_2()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("ENTER_USER");
		fixture.setApiKey("");
		fixture.setKeyFile("");

		fixture.validateKeyFileDefaultValue();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_3()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("ENTER_KEY");
		fixture.setKeyFile("");

		fixture.validateKeyFileDefaultValue();

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateKeyFileDefaultValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateKeyFileDefaultValue_4()
			throws Exception {
		final CloudUser fixture = new CloudUser();
		fixture.setUser("");
		fixture.setApiKey("");
		fixture.setKeyFile("ENTER_KEY_FILE_NAME");

		fixture.validateKeyFileDefaultValue();

		// add additional test code here
		// unverified
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 4/5/12 10:11 AM
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
	 * @generatedBy CodePro at 4/5/12 10:11 AM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}