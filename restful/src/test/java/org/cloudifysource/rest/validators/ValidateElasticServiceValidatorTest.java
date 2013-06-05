package org.cloudifysource.rest.validators;

import java.util.Properties;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.properties.BeanLevelProperties;

/**
 * The class <code>ValidateElasticServiceValidatorTest</code> contains tests for the class
 * <code>{@link org.cloudifysource.rest.validators.ValidateElasticServiceValidator}</code>.
 *
 * @generatedBy CodePro at 6/3/13 10:29 AM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ValidateElasticServiceValidatorTest {
	/**
	 * Run the void validate(SetServiceInstancesValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 6/3/13 10:29 AM
	 */
	@Test
	public void testElasticPu()
			throws Exception {
		ValidateElasticServiceValidator fixture = new ValidateElasticServiceValidator();

		SetServiceInstancesValidationContext validationContext = new SetServiceInstancesValidationContext();
		validationContext.setApplicationName("default");
		validationContext.setServiceName("simple");
		validationContext.setCloud(null);

		ProcessingUnit mockedPu = Mockito.mock(ProcessingUnit.class);
		BeanLevelProperties mockedBlp = Mockito.mock(BeanLevelProperties.class);

		Properties props = new Properties();
		props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC, "true");

		Mockito.when(mockedBlp.getContextProperties()).thenReturn(props);
		Mockito.when(mockedPu.getBeanLevelProperties()).thenReturn(mockedBlp);

		validationContext.setProcessingUnit(mockedPu);

		fixture.validate(validationContext);

	}

	/**
	 * Run the void validate(SetServiceInstancesValidationContext) method test.
	 *
	 * @throws Exception
	 *
	 * @generatedBy CodePro at 6/3/13 10:29 AM
	 */
	@Test(expected = RestErrorException.class)
	public void testNonElasticPu()
			throws Exception {
		ValidateElasticServiceValidator fixture = new ValidateElasticServiceValidator();

		SetServiceInstancesValidationContext validationContext = new SetServiceInstancesValidationContext();
		validationContext.setApplicationName("default");
		validationContext.setServiceName("simple");
		validationContext.setCloud(null);

		ProcessingUnit mockedPu = Mockito.mock(ProcessingUnit.class);
		BeanLevelProperties mockedBlp = Mockito.mock(BeanLevelProperties.class);

		Properties props = new Properties();
		props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC, "false");

		Mockito.when(mockedBlp.getContextProperties()).thenReturn(props);
		Mockito.when(mockedPu.getBeanLevelProperties()).thenReturn(mockedBlp);

		validationContext.setProcessingUnit(mockedPu);

		fixture.validate(validationContext);

	}

	@Test(expected = RestErrorException.class)
	public void testDefaultElasticValuePu()
			throws Exception {
		ValidateElasticServiceValidator fixture = new ValidateElasticServiceValidator();

		SetServiceInstancesValidationContext validationContext = new SetServiceInstancesValidationContext();
		validationContext.setApplicationName("default");
		validationContext.setServiceName("simple");
		validationContext.setCloud(null);

		ProcessingUnit mockedPu = Mockito.mock(ProcessingUnit.class);
		BeanLevelProperties mockedBlp = Mockito.mock(BeanLevelProperties.class);

		Properties props = new Properties();
		// props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC, "false");

		Mockito.when(mockedBlp.getContextProperties()).thenReturn(props);
		Mockito.when(mockedPu.getBeanLevelProperties()).thenReturn(mockedBlp);

		validationContext.setProcessingUnit(mockedPu);

		fixture.validate(validationContext);

	}

	/**
	 * Perform pre-test initialization.
	 *
	 * @throws Exception
	 *             if the initialization fails for some reason
	 *
	 * @generatedBy CodePro at 6/3/13 10:29 AM
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
	 * @generatedBy CodePro at 6/3/13 10:29 AM
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
	 * @generatedBy CodePro at 6/3/13 10:29 AM
	 */
	public static void main(final String[] args) {
		new org.junit.runner.JUnitCore().run(ValidateElasticServiceValidatorTest.class);
	}
}