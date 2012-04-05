package org.cloudifysource.dsl.autoscaling;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Domain Object for defining SLA based on thresholds that triggers 
 * scale in action.
 * 
 * @author elip
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */

@CloudifyDSLEntity(name = "lowThreshold" , clazz = LowThreshold.class , 
		allowInternalNode = true , allowRootNode = false , parent = "scalingRules")
public class LowThreshold {

	private Comparable<?> value;
	private int instancesDecrease;
	
	public Comparable<?> getValue() {
		return value;
	}
	public void setValue(Comparable<?> value) {
		this.value = value;
	}
	public int getInstancesDecrease() {
		return instancesDecrease;
	}
	public void setDecreaseNumOfInstances(int instancesDecrease) {
		this.instancesDecrease = instancesDecrease;
	}
	
	
}
