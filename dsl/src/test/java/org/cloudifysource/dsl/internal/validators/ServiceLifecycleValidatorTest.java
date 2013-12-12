package org.cloudifysource.dsl.internal.validators;

import groovy.lang.Closure;

import java.io.File;
import java.util.HashMap;

import org.cloudifysource.domain.ServiceLifecycle;
import org.cloudifysource.dsl.entry.ExecutableDSLEntryFactory;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.*;

/**
 * The class <code>ServiceLifecycleValidatorTest</code> contains tests for the class <code>{@link ServiceLifecycleValidator}</code>.
 *
 * @generatedBy CodePro at 12/11/13 5:43 PM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ServiceLifecycleValidatorTest {
	/**
	 * Run the void setDSLEntity(Object) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testSetDSLEntity_1()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		fixture.setDSLEntity(new ServiceLifecycle());
		Object dslEntity = new ServiceLifecycle();

		fixture.setDSLEntity(dslEntity);

		// add additional test code here
	}

	/**
	 * Run the void validateMonitosIsClosureOrMap(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testValidateMonitosIsClosureOrMap_1()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		fixture.setDSLEntity(new ServiceLifecycle());
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateMonitorsIsClosureOrMap(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateMonitosIsClosureOrMap(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test(expected = DSLValidationException.class)
	public void testValidateMonitosIsClosureOrMap_2()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		ServiceLifecycle dslEntity = new ServiceLifecycle();
		dslEntity.setMonitors(ExecutableDSLEntryFactory.createEntry("x.groovy", "monitors", new File(".")));
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateMonitorsIsClosureOrMap(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateMonitosIsClosureOrMap(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testValidateMonitosIsClosureOrMap_3()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		ServiceLifecycle dslEntity = new ServiceLifecycle();
		dslEntity.setMonitors(new HashMap<Object, Object>());
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateMonitorsIsClosureOrMap(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateMonitosIsClosureOrMap(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testValidateMonitosIsClosureOrMap_4()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		ServiceLifecycle dslEntity = new ServiceLifecycle();
		Closure<Object> closure = new Closure<Object>(null) {
			private static final long serialVersionUID = 1L;
		};
		
		dslEntity.setMonitors(closure);
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateMonitorsIsClosureOrMap(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateStopDetectorIsClosure(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testValidateStopDetectorIsClosure_1()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		fixture.setDSLEntity(new ServiceLifecycle());
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateStopDetectorIsClosure(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateStopDetectorIsClosure(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test(expected = DSLValidationException.class)
	public void testValidateStopDetectorIsClosure_2()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		ServiceLifecycle dslEntity = new ServiceLifecycle();
		dslEntity.setStopDetection(ExecutableDSLEntryFactory.createEntry("x.groovy", "stopDetection", new File(".")));
		fixture.setDSLEntity(dslEntity);
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateStopDetectorIsClosure(validationContext);

		// add additional test code here
	}

	/**
	 * Run the void validateStopDetectorIsClosure(DSLValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	@Test
	public void testValidateStopDetectorIsClosure_3()
		throws Exception {
		ServiceLifecycleValidator fixture = new ServiceLifecycleValidator();
		ServiceLifecycle dslEntity = new ServiceLifecycle();
		dslEntity.setStopDetection(ExecutableDSLEntryFactory.createEntry(new Closure<Object>(null) {
			private static final long serialVersionUID = 1L;
		}, "monitors", new File(".")));
		fixture.setDSLEntity(dslEntity);
		
		DSLValidationContext validationContext = new DSLValidationContext();

		fixture.validateStopDetectorIsClosure(validationContext);

		// add additional test code here
	}

	/**
	 * Perform pre-test initialization.
	 *
	 * @throws Exception
	 *         if the initialization fails for some reason
	 *
	 * @generatedBy CodePro at 12/11/13 5:43 PM
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
	 * @generatedBy CodePro at 12/11/13 5:43 PM
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
	 * @generatedBy CodePro at 12/11/13 5:43 PM
	 */
	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(ServiceLifecycleValidatorTest.class);
	}
}