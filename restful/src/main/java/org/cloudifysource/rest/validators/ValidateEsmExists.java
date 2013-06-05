package org.cloudifysource.rest.validators;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;

/**
 *
 * @author yael
 *
 */
public class ValidateEsmExists implements InstallServiceValidator {

    private static final int TIMEOUT = 5000;

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        Admin admin = validationContext.getAdmin();
        final ElasticServiceManager esm = getESM(admin);
        if (esm == null) {
            throw new RestErrorException(CloudifyMessageKeys.ESM_MISSING.getName(), Arrays.toString(admin.getGroups()));
        }
    }


    private ElasticServiceManager getESM(final Admin admin) {
        return admin.getElasticServiceManagers().waitForAtLeastOne(TIMEOUT,
                TimeUnit.MILLISECONDS);
    }

}
