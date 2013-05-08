package org.cloudifysource.rest.validators;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Before;
import org.junit.Test;

public class ValidateCloudConfigurationFileSizeTest extends InstallServiceValidatorTest {

	private ValidateCloudConfigurationFileSize validator;	
	private static final long TEST_FILE_SIZE_LIMIT = 3;
	
	
	@Before
	public void initValidator() {
		validator = new ValidateCloudConfigurationFileSize();
		validator.setCloudConfigurationFileSizeLimit(TEST_FILE_SIZE_LIMIT);
	}
	
	@Test
	public void testSizeLimitExeeded() throws IOException {
		File cloudConfig = File.createTempFile("cloudConfig", "");
		FileUtils.writeStringToFile(cloudConfig, "I'm longer than 3 bytes !");
		testValidator(null, null, null, null, null, null, cloudConfig, 
				CloudifyMessageKeys.CLOUD_CONFIGURATION_SIZE_LIMIT_EXCEEDED.getName());
		cloudConfig.delete();
	}
	
	@Override
	public InstallServiceValidator getValidatorInstance() {
		return validator;
	}

}
