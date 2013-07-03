package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.StatelessProcessingUnit;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

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
