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
	private Comparable<?> decrease;
	
	public Comparable<?> getValue() {
		return value;
	}
	public void setValue(Comparable<?> value) {
		this.value = value;
	}
	public Comparable<?> getDecrease() {
		return decrease;
	}
	public void setDecrease(Comparable<?> decrease) {
		this.decrease = decrease;
	}
	
	
}
