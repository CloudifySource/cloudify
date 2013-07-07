package org.cloudifysource.dsl.internal.validators;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.UsmComponent;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class UsmComponentValidator extends GridComponentValidator implements DSLValidator {

	private UsmComponent entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (UsmComponent) dslEntity;
	}
	
	@DSLValidation
	public void validatePortRange(final DSLValidationContext validationContext) throws DSLValidationException {
		if (StringUtils.isEmpty(this.entity.getPortRange())) {
			throw new DSLValidationException("LRMI port can't be null");
		}
		String[] range = this.entity.getPortRange().split("-");
		if (range.length != 2) {
			throw new DSLValidationException("LRMI port range should be set as '<START_PORT>-<END_PORT>'");
		}
		Integer startPort = parseInt(range[0]);
		super.validatePort(startPort);
		Integer endPort = parseInt(range[1]);
		super.validatePort(endPort);
		
		if (startPort > endPort) {
			throw new DSLValidationException("start port must be greater than end port");
		}
	}
	
	@DSLValidation
	public void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}

	private int parseInt(final String number) throws DSLValidationException {
		if (!isNumeric(number)) {
			throw new DSLValidationException("port range must be a number. found " + number);
		}
		return Integer.parseInt(number);
	}
	
	private boolean isNumeric(final String str) {
	  return str.matches("\\d*");  
	}

}
