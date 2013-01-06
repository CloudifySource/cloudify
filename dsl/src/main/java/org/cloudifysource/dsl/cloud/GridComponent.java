package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/******
 * Abstract grid component configuration.
 * 
 * @author adaml
* @since 2.5.0
 */
public abstract class GridComponent {
	
	private String minMemory;
	private String maxMemory;
	private Integer port;

	public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	} 
	
	public String getMinMemory() {
		return minMemory;
	}
	
	public void setMinMemory(final String minMemory) {
		this.minMemory = minMemory;
	}
	
	public String getMaxMemory() {
		return maxMemory;
	}
	
	public void setMaxMemory(final String maxMemory) {
		this.maxMemory = maxMemory;
	}
	
	//TODO [adaml]: add memory format validation.
	@DSLValidation
	void validateMemorySyntax(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		
//		validateNumberPrefix();
//		validateMemorySuffix();
	}
	
//	private void validateIsNumeric(final String maxMemoryPrefix)
//			throws DSLValidationException {
//		if (!StringUtils.isNumeric(maxMemoryPrefix) || maxMemoryPrefix.length() == 0) {
//			throw new DSLValidationException("Component memory alloaction should be defined as such:" 
//					+ " <NUMBER>m.");
//		}
//	}

	@DSLValidation
	void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		if (this.port != null) {
			if (this.port <= 0) {
				throw new DSLValidationException("Port must be set to a positive number." 
						+ " set " + this.port.toString());
			}
		}
	}
	
}
