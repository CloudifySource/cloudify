package org.cloudifysource.dsl;

import java.util.Map;
/**
 * The abstract class ServiceProccessingUnit holds all the data shared 
 * by the different processing units i.e. DataGrid, StatelessPU, StatefulPU and Memcached.
 * Specifically holds the Processing unit's SLA and it's context properties.
 * 
 * @author adaml
 *
 */

public abstract class ServiceProcessingUnit {

	private Sla sla;
	private Map<String, String> contextProperties;

	
	/**
	 * Returns the SLA object as defined in the groovy service file.
	 * The SLA holds in it the JVM's memory and availability definitions.
	 * @return a processing unit's SLA object.
	 */
	public Sla getSla() {
		return sla;
	}

	/**
	 * returns a Map that holds all of the processing unit's context properties.
	 * @param contextProperties - a Map object holding all of the PU context properties.
	 */
	public Map<String, String> getContextProperties() {
		return contextProperties;
	}
	
	public void setContextProperties(Map<String, String> contextProperties) {
		this.contextProperties = contextProperties;
	}
	
	public void setSla(Sla sla) {
		this.sla = sla;
	}

}
