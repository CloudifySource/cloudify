package org.cloudifysource.dsl.cloud;

import java.util.HashMap;

import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CloudTemplateTest</code> contains tests for the class <code>{@link ComputeTemplate}</code>.
 * 
 * @generatedBy CodePro at 5/12/12 1:01 AM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class CloudTemplateTest {

	/**
	 * Run the void validateDefaultValues() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/12/12 1:01 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateDefaultValues_1()
			throws Exception {
		final ComputeTemplate fixture = new ComputeTemplate();
		fixture.setRemoteExecution(RemoteExecutionModes.SSH);
		fixture.setImageId("");
		fixture.setOptions(new HashMap());
		fixture.setHardwareId("");
		fixture.setRemoteDirectory((String) null);
		fixture.setNumberOfCores(1);
		fixture.setFileTransfer(FileTransferModes.CIFS);
		fixture.setCustom(new HashMap());
		fixture.setUsername("");
		fixture.setLocationId("");
		fixture.setOverrides(new HashMap());
		fixture.setPassword("");
		fixture.setMachineMemoryMB(1);

		fixture.validateDefaultValues(new DSLValidationContext());

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateDefaultValues() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/12/12 1:01 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateDefaultValues_2()
			throws Exception {
		final ComputeTemplate fixture = new ComputeTemplate();
		fixture.setRemoteExecution(RemoteExecutionModes.SSH);
		fixture.setImageId("");
		fixture.setOptions(new HashMap());
		fixture.setHardwareId("");
		fixture.setRemoteDirectory("");
		fixture.setNumberOfCores(1);
		fixture.setFileTransfer(FileTransferModes.CIFS);
		fixture.setCustom(new HashMap());
		fixture.setUsername("");
		fixture.setLocationId("");
		fixture.setOverrides(new HashMap());
		fixture.setPassword("");
		fixture.setMachineMemoryMB(1);

		fixture.validateDefaultValues(new DSLValidationContext());

		// add additional test code here
		// unverified
	}

	/**
	 * Run the void validateDefaultValues() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 5/12/12 1:01 AM
	 */
	@Test(expected = org.cloudifysource.dsl.internal.DSLValidationException.class)
	public void testValidateDefaultValues_3()
			throws Exception {
		final ComputeTemplate fixture = new ComputeTemplate();
		fixture.setRemoteExecution(RemoteExecutionModes.SSH);
		fixture.setImageId("");
		fixture.setOptions(new HashMap());
		fixture.setHardwareId("");
		fixture.setRemoteDirectory("");
		fixture.setNumberOfCores(1);
		fixture.setFileTransfer(FileTransferModes.CIFS);
		fixture.setCustom(new HashMap());
		fixture.setUsername("");
		fixture.setLocationId("");
		fixture.setOverrides(new HashMap());
		fixture.setPassword("");
		fixture.setMachineMemoryMB(1);

		fixture.validateDefaultValues(new DSLValidationContext());

		// add additional test code here
		// unverified
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 5/12/12 1:01 AM
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
	 * @generatedBy CodePro at 5/12/12 1:01 AM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}