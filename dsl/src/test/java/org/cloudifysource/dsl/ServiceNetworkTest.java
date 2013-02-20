
package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//CHECKSTYLE:OFF

/**
 * The class <code>ServiceNetworkTest</code> contains tests for the class <code>{@link ServiceNetwork}</code>.
 * 
 * @generatedBy CodePro at 5/24/12 2:58 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ServiceNetworkTest {

	/**
	 * Run the void checkDescription() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/24/12 2:58 PM
	 */
	@Test
	public void testCheckDescription_1()
			throws Exception {
		final ServiceNetwork fixture = new ServiceNetwork();
		fixture.setPort(1);
		fixture.setProtocolDescription("");

		fixture.checkDescription(new DSLValidationContext());

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void checkDescription() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/24/12 2:58 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckDescription_2()
			throws Exception {
		final ServiceNetwork fixture = new ServiceNetwork();
		fixture.setPort(1);
		fixture.setProtocolDescription((String) null);

		fixture.checkDescription(new DSLValidationContext());

	}

	/**
	 * Run the void checkPortValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/24/12 2:58 PM
	 */
	@Test
	public void testCheckPortValue_1()
			throws Exception {
		final ServiceNetwork fixture = new ServiceNetwork();
		fixture.setPort(1);
		fixture.setProtocolDescription("");

		fixture.checkPortValue(new DSLValidationContext());

	}

	/**
	 * Run the void checkPortValue() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/24/12 2:58 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckPortValue_2()
			throws Exception {
		final ServiceNetwork fixture = new ServiceNetwork();
		fixture.setPort(0);
		fixture.setProtocolDescription("");

		fixture.checkPortValue(new DSLValidationContext());

	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 5/24/12 2:58 PM
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
	 * @generatedBy CodePro at 5/24/12 2:58 PM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}