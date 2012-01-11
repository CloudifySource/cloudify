package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Defines an elastic deployment of a processing unit that contains an embedded space.
 * The steteful Processing unit configuration POJO is initialized by 
 * the service groovy DSL and holds all of the required information regarding 
 * the deployment of stateful processing units.
 *  
 * @see ElasticStatefulProcessingUnitDeployment
 * 
 * @author adaml
 *
 */
@CloudifyDSLEntity(name="statefulProcessingUnit", clazz=StatefulProcessingUnit.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class StatefulProcessingUnit extends ServiceProcessingUnit {
	
	private String binaries;
	
	/**
	 * can be a folder, or a jar/war file.   
	 * @return - a String containing the folder path or the jar file name.
	 */
	public String getBinaries() {
		return binaries;
	}
	
	public void setBinaries(String binaries) {
		this.binaries = binaries;
	}
}
