package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class MirrorProcessingUnitValidator 
						extends StatelessProcessingUnitValidator implements DSLValidator {

	@Override
	public void setDSLEntity(final Object dslEntity) {
		super.setDSLEntity(dslEntity);
	}
	
	@DSLValidation
	void validateStatelessSLA(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		super.validateStatelessSLA(validationContext);
	}

}
