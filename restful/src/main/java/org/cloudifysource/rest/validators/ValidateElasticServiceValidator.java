/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.rest.validators;

import java.util.Properties;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.springframework.stereotype.Component;

/*************
 * Validates that a service is marked as elastic.
 * @author barakme
 * @since 2.6.0
 *
 */
@Component
public class ValidateElasticServiceValidator implements SetServiceInstancesValidator {
	
	private static final Logger logger = Logger.getLogger(ValidateElasticServiceValidator.class.getName());

	@Override
	public void validate(final SetServiceInstancesValidationContext validationContext) throws RestErrorException {
		logger.info("Validating elastic service");
		final ProcessingUnit pu = validationContext.getProcessingUnit();
		final Properties contextProperties = pu.getBeanLevelProperties()
				.getContextProperties();
		final String elasticProp = contextProperties
				.getProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC);

		if (elasticProp == null || !Boolean.parseBoolean(elasticProp)) {
			throw new RestErrorException(ResponseConstants.SERVICE_NOT_ELASTIC,
					validationContext.getServiceName());
		}

	}

}
