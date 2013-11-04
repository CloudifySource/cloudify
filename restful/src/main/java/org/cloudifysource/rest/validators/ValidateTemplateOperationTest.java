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

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class ValidateTemplateOperationTest {

	@Test
	public void testAddTemplatesOnLocalcloud() {
		AddTemplatesValidationContext validationContext = new AddTemplatesValidationContext();
		validationContext.setCloud(null);
		validationContext.setOperationName("add-templates");
		AddTemplatesRequest request = new AddTemplatesRequest();
		request.setUploadKey("key");
		validationContext.setRequest(request);
		
		ValidateTemplateOperation validator = new ValidateTemplateOperation();
		try {
			validator.validate(validationContext);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(CloudifyErrorMessages.ILLEGAL_TEMPLATE_OPERATION_ON_LOCAL_CLOUD.getName(), 
					e.getMessage());
		}
	}
}
