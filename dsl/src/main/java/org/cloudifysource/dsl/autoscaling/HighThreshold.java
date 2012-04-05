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
	private Comparable<?> increase;
	
	public Comparable<?> getValue() {
		return value;
	}
	public void setValue(Comparable<?> value) {
		this.value = value;
	}
	public Comparable<?> getIncrease() {
		return increase;
	}
	public void setIncrease(Comparable<?> increase) {
		this.increase = increase;
	}

}
