package org.cloudifysource.dsl.cloud;


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
}
