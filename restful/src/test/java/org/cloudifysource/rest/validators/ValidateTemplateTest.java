/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ValidateTemplateTest {

    private static final String CLOUD_FILE_PATH = ValidatorsTestsUtils.FOLDER + "/byon/byon-cloud.groovy";
    private static final String NO_COMPUTE_SERVICE = ValidatorsTestsUtils.FOLDER + "/simple.groovy";
    private static final String NOT_EXIST_TEMPLATE_SERVICE_GROOVY = 
    		ValidatorsTestsUtils.FOLDER + "/template5service.groovy";
	private static final String ERR_MSG = CloudifyErrorMessages.MISSING_TEMPLATE.getName();
	
    private ValidateTemplate validateTempalte;
	private ValidateApplicationServices validateApplicationServices;
	private Cloud cloud;

    @Before
    public void init() {
    	validateTempalte = new ValidateTemplate();
    	validateApplicationServices = new ValidateApplicationServices();
    	InstallServiceValidator[] installServiceValidators = {validateTempalte};
		validateApplicationServices.setInstallServiceValidators(installServiceValidators);
		
        try {
			cloud = ServiceReader.readCloud(new File(CLOUD_FILE_PATH));
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
    }
    
    @Test
    public void testMissingTemplate() throws DSLException, PackagingException {
        Service service = ServiceReader.readService(new File(NOT_EXIST_TEMPLATE_SERVICE_GROOVY));        
        InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
        validationContext.setCloud(cloud);
        validationContext.setService(service);
		ValidatorsTestsUtils.validate(validateTempalte, validationContext, ERR_MSG);
    }

    @Test
    public void testNullCompute() throws DSLException, PackagingException {
        Service service = ServiceReader.readService(new File(NO_COMPUTE_SERVICE));
        InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
        validationContext.setCloud(cloud);
        validationContext.setService(service);
		ValidatorsTestsUtils.validate(validateTempalte, validationContext, null);
    }

    @Test
    public void testMissingTemplateOnInstallApplication() throws DSLException, PackagingException {
        Service service = ServiceReader.readService(new File(NOT_EXIST_TEMPLATE_SERVICE_GROOVY));      
        
        InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
        validationContext.setCloud(cloud);
        Application application = new Application();
        application.setService(service);
		validationContext.setApplication(application);
		
		ValidatorsTestsUtils.validate(validateApplicationServices, validationContext, ERR_MSG);
    }

    @Test
    public void testNullComputeOnInstallApplication() throws DSLException, PackagingException {
        Service service = ServiceReader.readService(new File(NO_COMPUTE_SERVICE));
        
        InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
        validationContext.setCloud(cloud);
        Application application = new Application();
        application.setService(service);
		validationContext.setApplication(application);
		
		ValidatorsTestsUtils.validate(validateApplicationServices, validationContext, null);
    }

}
