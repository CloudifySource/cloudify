package org.cloudifysource.dsl.cloud;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.DSLValidationException;

/******
 * Abstract grid component configuration.
 * 
 * @author adaml
* @since 2.5.0
 */
public abstract class GridComponent {
	private static final int MAX_MEMORY = 65535;
	private static final int MIN_PORT = 1024;
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
	
	/**
	 * validates memory syntax
	 * 
	 * @throws DSLValidationException
	 * 			if memory syntax not valid
	 */
	protected void validateMemorySyntax() 
			throws DSLValidationException {
		validateMemoryString(this.maxMemory);
		validateMemoryString(this.minMemory);
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
				|| !StringUtils.isNumeric(this.maxMemory.substring(0, this.maxMemory.length() - 1))) {
				
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
