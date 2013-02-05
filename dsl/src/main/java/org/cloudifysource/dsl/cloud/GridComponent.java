package org.cloudifysource.dsl.cloud;

import org.apache.commons.lang.StringUtils;
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
	
	@DSLValidation
	void validateMemorySyntax(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		if (this.maxMemory == null 
			|| this.maxMemory.length() <= 1
			|| this.maxMemory.endsWith("m")
			|| !StringUtils.isNumeric(this.maxMemory.substring(0, this.maxMemory.length() - 1))) {
			
				throw new DSLValidationException("Illegal memory property: " + this.maxMemory
						+ " Memory property should be defined as '<NUMBER>m'.");
		}
		if (this.minMemory == null 
			|| this.minMemory.length() <= 1
			|| this.minMemory.endsWith("m")
			|| !StringUtils.isNumeric(this.minMemory.substring(0, this.minMemory.length() - 1))) {
			
				throw new DSLValidationException("Illegal memory property: " + this.minMemory
						+ " Memory property should be defined as '<NUMBER>m'.");
			}
	}
}
