package org.cloudifysource.dsl.autoscaling;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Domain Object for defining SLA based on thresholds that triggers 
 * scale out action.
 * 
 * @author elip
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */

@CloudifyDSLEntity(name = "highThreshold" , clazz = HighThreshold.class , 
		allowInternalNode = true , allowRootNode = false , parent = "scalingRules")
public class HighThreshold {

	private Comparable<?> value;
	private int instancesIncrease;
	
	public Comparable<?> getValue() {
		return value;
	}
	public void setValue(final Comparable<?> value) {
		this.value = value;
	}
	public int getInstancesIncrease() {
		return instancesIncrease;
	}
	public void setInstancesIncrease(final int instancesIncrease) {
		this.instancesIncrease = instancesIncrease;
	}

}
