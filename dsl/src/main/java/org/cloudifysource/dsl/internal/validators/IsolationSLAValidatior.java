package org.cloudifysource.dsl.internal.validators;

import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.IsolationSLA;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class IsolationSLAValidatior implements DSLValidator {

	private IsolationSLA entity;
	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (IsolationSLA) dslEntity;
	}
	
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		
		List<Object> notNulls = new ArrayList<Object>();
		
		if (entity.getGlobal() != null) {
			notNulls.add(entity.getGlobal());
		}
		if (entity.getDedicated() != null) {
			notNulls.add(entity.getDedicated());
		}
		if (entity.getAppShared() != null) {
			notNulls.add(entity.getGlobal());
		}
		if (entity.getTenantShared() != null) {
			notNulls.add(entity.getGlobal());
		}
		
		if (notNulls.size() > 1) {
			throw new DSLValidationException("cannot define two types of isolation sla's. please choose one");
		}
	}

}
