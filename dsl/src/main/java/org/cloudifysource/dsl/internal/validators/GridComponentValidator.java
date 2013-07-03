package org.cloudifysource.dsl.internal.validators;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.GridComponent;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class GridComponentValidator implements DSLValidator {

	private static final int MAX_MEMORY = 65535;
	private static final int MIN_PORT = 1024;
	
	private GridComponent entity; 
	
	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (GridComponent) dslEntity;
	}
	
	/**
	 * validates memory syntax
	 * 
	 * @throws DSLValidationException
	 * 			if memory syntax not valid
	 */
	protected void validateMemorySyntax() 
			throws DSLValidationException {
		validateMemoryString(this.entity.getMaxMemory());
		validateMemoryString(this.entity.getMinMemory());
	}

	private void validateMemoryString(final String memoryString) 
				throws DSLValidationException {
		if (memoryString == null 
				|| memoryString.length() <= 1
				|| (!memoryString.endsWith("m") 
						&& !memoryString.endsWith("g") 
						&& !memoryString.endsWith("t") 
						&& !memoryString.endsWith("p") 
						&& !memoryString.endsWith("e"))
				|| !StringUtils.isNumeric(this.entity.getMaxMemory().substring(0, 
						this.entity.getMaxMemory().length() - 1))) {
				
					throw new DSLValidationException("Illegal memory property: " + memoryString
							+ " Memory property should be defined as '<NUMBER><MEMORYUNIT>'.");
			}		
	}
	
	/**
	 * validates port is valid.
	 * 
	 * @param port
	 * 		the port to validate
	 * 
	 * @throws DSLValidationException
	 * 			if port not valid
	 */
	protected void validatePort(final Integer port) throws DSLValidationException {
		if (port == null) {
			throw new DSLValidationException("LRMI port can't be null");
		}
		if (port <= MIN_PORT || port > MAX_MEMORY) {
			throw new DSLValidationException("port must be set to a positive integer between"
					+ " 1024 and 65535. Instead found " + port);
		}
	}

}
