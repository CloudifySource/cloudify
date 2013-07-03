package org.cloudifysource.dsl.internal;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.AgentComponent;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.UsmComponent;
import org.cloudifysource.dsl.internal.validators.AgentComponentValidator;
import org.cloudifysource.dsl.internal.validators.UsmComponentValidator;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class DSLValidateObjectsTest {

private final static String CLOUD_PATH = "../esc/src/main/resources/clouds/rsopenstack/rsopenstack-cloud.groovy";
private final static String SERVICE_PATH = "testResources/simple/simple-service.groovy";

	//@Test
	public void testCloud() throws IOException {
		File cloudFile = new File(CLOUD_PATH);
		
		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(cloudFile);
		dslReader.setWorkDir(cloudFile.getParentFile());
		dslReader.setCreateServiceContext(false);
		
		try {
			dslReader.readDslEntity(Cloud.class);
			Assert.fail("The validation of file " + CLOUD_PATH + " was supposed to fail");
		} catch (Exception e) {
			
		}
		try {
			dslReader.setValidateObjects(false);
			dslReader.readDslEntity(Cloud.class);
		} catch (Exception e) {
			Assert.fail("Expecting no validation for cloud file " + CLOUD_PATH + ", exception was " + e.getMessage());
		}
	}
	
	@Test
	public void testGridComponentValidation() {
		final AgentComponent agentComponent = new AgentComponent();
		final AgentComponentValidator agentComponentValidator = new AgentComponentValidator();
		agentComponentValidator.setDSLEntity(agentComponent);
		agentComponent.setMaxMemory("128u");
		
		try {
			agentComponentValidator.validateMemory(new DSLValidationContext());
			Assert.fail("invalid memory passed dsl validation");
		} catch (final DSLValidationException e) {
			// OK - the invalid memory format caused the exception
		}
		
		agentComponent.setMaxMemory("128m");
		try {
			agentComponentValidator.validateMemory(new DSLValidationContext());
		} catch (final DSLValidationException e) {
			Assert.fail("legit memory failed dsl validation");
		}

		agentComponent.setPort(124);
		try {
			agentComponentValidator.validatePort(new DSLValidationContext());
			Assert.fail("invalid port passed dsl validation");
		} catch (final DSLValidationException e) {
			//OK - port is not in the port range.
		}
		
		agentComponent.setPort(7000);
		try {
			agentComponentValidator.validateMemory(new DSLValidationContext());
		} catch (final DSLValidationException e) {
			Assert.fail("legit port failed dsl validation");
		}
		
		UsmComponent usmComponent = new UsmComponent();
		UsmComponentValidator usmComponentValidator = new UsmComponentValidator();
		usmComponentValidator.setDSLEntity(usmComponent);
		usmComponent.setPortRange("7000-7100");
		try {
			usmComponentValidator.validatePortRange(new DSLValidationContext());
		} catch (final DSLValidationException e) {
			Assert.fail("legit port range failed dsl validation");
		}
		
		usmComponent.setPortRange("700q-7100");
		try {
			usmComponentValidator.validatePortRange(new DSLValidationContext());
			Assert.fail("invalid port range passed dsl validation");
		} catch (final DSLValidationException e) {
			//OK
		}
		
		usmComponent.setPortRange("7100-7000");
		try {
			usmComponentValidator.validatePortRange(new DSLValidationContext());
			Assert.fail("invalid port range passed dsl validation");
		} catch (final DSLValidationException e) {
			//OK
		}
		
		usmComponent.setPortRange("102-700");
		try {
			usmComponentValidator.validatePortRange(new DSLValidationContext());
			Assert.fail("invalid port range passed dsl validation");
		} catch (final DSLValidationException e) {
			//OK
		}
	}
	
	@Test
	public void testService() throws IOException {
		File serviceFile = new File(SERVICE_PATH);
		
		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(serviceFile);
		dslReader.setWorkDir(serviceFile.getParentFile());		
		
		try {
			dslReader.readDslEntity(Service.class);
			Assert.fail("The validation of service file " + SERVICE_PATH + " was supposed to fail");
		} catch (Exception e) {
			
		}
		try {
			dslReader.setValidateObjects(false);
			dslReader.readDslEntity(Service.class);
		} catch (Exception e) {
			Assert.fail("Expecting no Validation for service file " + SERVICE_PATH + " exception was " + e.getMessage());
		}
	}
}
