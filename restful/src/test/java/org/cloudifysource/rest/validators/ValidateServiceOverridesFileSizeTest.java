package org.cloudifysource.rest.validators;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Before;
import org.junit.Test;

public class ValidateServiceOverridesFileSizeTest extends InstallServiceValidatorTest {

    private ValidateServiceOverridesFileSize validator;
    private static final long TEST_FILE_SIZE_LIMIT = 3;


    @Before
    public void initValidator() {
        validator = new ValidateServiceOverridesFileSize();
        validator.setServiceOverridesFileSizeLimit(TEST_FILE_SIZE_LIMIT);
    }

    @Test
    public void testSizeLimitExeeded() throws IOException {
        File serviceOverrides = File.createTempFile("serviceOverrides", "");
        FileUtils.writeStringToFile(serviceOverrides, "I'm longer than 3 bytes !");
        testValidator(null, null, null, null, null, serviceOverrides, null,
                CloudifyMessageKeys.SERVICE_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName());
        serviceOverrides.delete();
    }

    @Override
    public InstallServiceValidator getValidatorInstance() {
        return validator;
    }

}
