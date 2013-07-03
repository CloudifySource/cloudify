package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

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
