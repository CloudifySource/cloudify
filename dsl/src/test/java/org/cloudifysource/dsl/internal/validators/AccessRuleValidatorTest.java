package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.domain.network.AccessRule;
import org.cloudifysource.domain.network.AccessRuleType;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.*;

/**
 * The class <code>AccessRuleValidatorTest</code> contains tests for the class <code>{@link AccessRuleValidator}</code>.
 *
 * @generatedBy CodePro at 10/21/13 8:17 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class AccessRuleValidatorTest {
	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckDefaultValues_1()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		fixture.setDSLEntity(new AccessRule());
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test
	public void testCheckDefaultValues_2()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80");
		dslEntity.setType(AccessRuleType.PUBLIC);
		
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckDefaultValues_3()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80");
		
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckDefaultValues_4()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80");
		dslEntity.setType(AccessRuleType.GROUP);
		
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testCheckDefaultValues_5()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80");
		dslEntity.setType(AccessRuleType.RANGE);
		
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void checkDefaultValues(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@Test
	public void testCheckDefaultValues_6()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80");
		dslEntity.setType(AccessRuleType.GROUP);
		dslEntity.setTarget("My_Group");
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkDefaultValues(validationContext);

		// add additional test code here
	}

	@Test(expected = DSLValidationException.class)
	public void testIllegalPortValues()
		throws Exception {
		AccessRuleValidator fixture = new AccessRuleValidator();
		AccessRule dslEntity = new AccessRule();
		dslEntity.setPortRange("80-aaa");
		dslEntity.setType(AccessRuleType.GROUP);
		dslEntity.setTarget("My_Group");
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.checkPortRange(validationContext);

		// add additional test code here
	}



	/**
	 * Perform pre-test initialization.
	 *
	 * @throws Exception
	 *         if the initialization fails for some reason
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
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
	 *         if the clean-up fails for some reason
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	@After
	public void tearDown()
		throws Exception {
		// Add additional tear down code here
	}

	/**
	 * Launch the test.
	 *
	 * @param args the command line arguments
	 *
	 * @generatedBy CodePro at 10/21/13 8:17 PM
	 */
	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(AccessRuleValidatorTest.class);
	}
}