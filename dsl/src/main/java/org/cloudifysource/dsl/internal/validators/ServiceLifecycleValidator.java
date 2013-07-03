package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.dsl.entry.ClosureExecutableEntry;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class ServiceLifecycleValidator implements DSLValidator {

	private ServiceLifecycle entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (ServiceLifecycle) dslEntity;
	}
	
	@DSLValidation
	void validateStopDetectorIsClosure(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if ((this.entity.getStopDetection() != null)
				&& (!(this.entity.getStopDetection() instanceof ClosureExecutableEntry))) {
			throw new DSLValidationException(
					"The stop detection field only supports execution of closures");
		}
	}
}
