package org.cloudifysource.rest.validators;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Before;
import org.junit.Test;

public class ValidateCloudOverridesFileSizeTest extends InstallServiceValidatorTest {

	private ValidateCloudOverridesFileSize validator;	
	private static final long TEST_FILE_SIZE_LIMIT = 3;
	
	
	@Before
	public void initValidator() {
		validator = new ValidateCloudOverridesFileSize();
		validator.setCloudOverridesFileSizeLimit(TEST_FILE_SIZE_LIMIT);
	}
	
	@Test
	public void testSizeLimitExeeded() throws IOException {
		File cloudOverrides = File.createTempFile("cloudOverrides", "");
		FileUtils.writeStringToFile(cloudOverrides, "I'm longer than 3 bytes !");
		testValidator(null, null, null, null, cloudOverrides, null, null, 
				CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName());
		cloudOverrides.delete();
	}
	
	@Override
	public InstallServiceValidator getValidatorInstance() {
		return validator;
	}

}
