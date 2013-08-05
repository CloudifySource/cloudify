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
import org.cloudifysource.domain.Sla;
import org.cloudifysource.domain.StatelessProcessingUnit;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class StatelessProcessingUnitValidator implements DSLValidator {

	private StatelessProcessingUnit entity;
	
	public void setDSLEntity(final Object dslEntity) {
		this.entity = (StatelessProcessingUnit) dslEntity;
	}
	
	@DSLValidation
	void validateStatelessSLA(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		final Sla sla = entity.getSla();
		if (sla != null) {
			if (sla.getMemoryCapacity() != null) {
				throw new DSLValidationException(
						"memoryCapacity SLA is not supported in this service");
			}
			if (sla.getMaxMemoryCapacity() != null) {
				throw new DSLValidationException(
						"maxMemoryCapacity SLA is not supported in this service");
			}
		}
	}
}
