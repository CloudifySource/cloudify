package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class SharedIsolationSLADescriptor extends GlobalIsolationSLADescriptor {

	private String isolationId;
	
	public String getIsolationId() {
		return isolationId;
	}

	public void setIsolationId(final String isolationId) {
		this.isolationId = isolationId;
	}
	
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		super.validateDefaultValues(validationContext);
		
		if (isolationId == null) {
			throw new DSLValidationException("isolationId cannot be null");
		}
		
	}

	
}
