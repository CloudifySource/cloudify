package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;

public class ValidateCloudConfigurationFileSize implements InstallServiceValidator {

    private static final long DEFAULT_CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES =
            CloudifyConstants.CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES;
    private long cloudConfigurationFileSizeLimit = DEFAULT_CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES;

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        File cloudConfigurationFile = validationContext.getCloudConfigurationFile();
        if (cloudConfigurationFile != null) {
            if (cloudConfigurationFile.length() > cloudConfigurationFileSizeLimit) {
                throw new RestErrorException(CloudifyMessageKeys.CLOUD_CONFIGURATION_SIZE_LIMIT_EXCEEDED.getName(),
                        cloudConfigurationFile.getAbsolutePath());
            }
        }

    }

    public long getCloudConfigurationFileSizeLimit() {
        return cloudConfigurationFileSizeLimit;
    }

    public void setCloudConfigurationFileSizeLimit(final long cloudConfigurationFileSizeLimit) {
        this.cloudConfigurationFileSizeLimit = cloudConfigurationFileSizeLimit;
    }

}
