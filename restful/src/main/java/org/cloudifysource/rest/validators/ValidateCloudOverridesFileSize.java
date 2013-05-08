package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * Validate the size of cloud overrides file.
 * @author yael
 *
 */
public class ValidateCloudOverridesFileSize implements InstallServiceValidator {

	private static final long DEFAULT_CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES = 
			CloudifyConstants.CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
	private long cloudOverridesFileSizeLimit = DEFAULT_CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
	
	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		File cloudOverridesFile = validationContext.getCloudOverridesFile();
		if (cloudOverridesFile != null) {
			if (cloudOverridesFile.length() > cloudOverridesFileSizeLimit) {
				throw new RestErrorException(CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(), 
						cloudOverridesFile.getAbsolutePath());
			}
		}

	}

	public long getCloudOverridesFileSizeLimit() {
		return cloudOverridesFileSizeLimit;
	}

	public void setCloudOverridesFileSizeLimit(final long cloudOverridesFileSizeLimit) {
		this.cloudOverridesFileSizeLimit = cloudOverridesFileSizeLimit;
	}

}
