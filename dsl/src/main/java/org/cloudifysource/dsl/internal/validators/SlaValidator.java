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
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class SlaValidator implements DSLValidator {

	private Sla entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (Sla) dslEntity;
	}
	
	@DSLValidation
	void validateMemoryValues(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		
		if (this.entity.getMemoryCapacityPerContainer() == null) {
			throw new DSLValidationException(
					"Cannot determine memoryCapacityPerContainer SLA");
		}
		
		final int minimumNumberOfContainers = this.entity.getHighlyAvailable() ? 2 : 1;
		final int minMemoryCapacityRequired = minimumNumberOfContainers
				* this.entity.getMemoryCapacityPerContainer();
		
		if (this.entity.getMaxMemoryCapacity() != null
				&& entity.getMemoryCapacity() != null
				&& entity.getMaxMemoryCapacity() < entity.getMemoryCapacity()) {

			throw new DSLValidationException(
					"Max memory capacity is smaller than the minimal memory capacity required."
							+ entity.getMaxMemoryCapacity() + " < "
							+ entity.getMemoryCapacity());
		}
		if (this.entity.getMaxMemoryCapacity() != null) {
			if (this.entity.getMaxMemoryCapacity() < minMemoryCapacityRequired) {
				throw new DSLValidationException(
						"Cannot determine memoryCapacityPerContainer SLA");
			}
		}
	}

}
