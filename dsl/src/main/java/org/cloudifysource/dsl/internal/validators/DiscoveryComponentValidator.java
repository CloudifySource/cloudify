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
package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.cloud.DiscoveryComponent;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class DiscoveryComponentValidator extends GridComponentValidator implements DSLValidator {

	private DiscoveryComponent entity;

	@Override
	public void setDSLEntity(final Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (DiscoveryComponent) dslEntity;
	}
	
	@DSLValidation
	void validatePorts(final DSLValidationContext validationContext) throws DSLValidationException {
//		if (this.entity.getDiscoveryPort() == null) {
//			entity.setDiscoveryPort(CloudifyConstants.DEFAULT_LUS_PORT);
//		}
		super.validatePort(this.entity.getPort());
//		super.validatePort(this.entity.getDiscoveryPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
}
