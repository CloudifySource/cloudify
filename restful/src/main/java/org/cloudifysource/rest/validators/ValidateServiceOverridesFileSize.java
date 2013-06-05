package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * Validate the service overrides file size.
 * @author yael
 *
 */
public class ValidateServiceOverridesFileSize implements InstallServiceValidator {

    private static final long DEFAULT_SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES =
            CloudifyConstants.SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
    private long serviceOverridesFileSizeLimit = DEFAULT_SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        File serviceOverridesFile = validationContext.getServiceOverridesFile();
        if (serviceOverridesFile != null) {
            long length = serviceOverridesFile.length();
            if (length > serviceOverridesFileSizeLimit) {
                throw new RestErrorException(CloudifyMessageKeys.SERVICE_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(),
                        serviceOverridesFile.getAbsolutePath(), length, serviceOverridesFileSizeLimit);
            }
        }

    }

    public long getServiceOverridesFileSizeLimit() {
        return serviceOverridesFileSizeLimit;
    }

    public void setServiceOverridesFileSizeLimit(final long serviceOverridesFileSizeLimit) {
        this.serviceOverridesFileSizeLimit = serviceOverridesFileSizeLimit;
    }

}
