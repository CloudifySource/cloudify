package org.cloudifysource.domain.scalingrules;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/**
 * Domain Object for defining SLA based on thresholds that triggers 
 * scale in action.
 * 
 * @author elip
 * @since 2.1
 * @see org.cloudifysource.domain.Service
 */

@CloudifyDSLEntity(name = "lowThreshold" , clazz = LowThresholdDetails.class , 
		allowInternalNode = true , allowRootNode = false , parent = "scalingRule")
public class LowThresholdDetails {

	private Comparable<?> value;
	private int instancesDecrease;
	
	public Comparable<?> getValue() {
		return value;
	}
	
	/**
	 * @param value - the threshold value below which number of instances is decreased.
	 */
	public void setValue(final Comparable<?> value) {
		this.value = value;
	}
	
	public int getInstancesDecrease() {
		return instancesDecrease;
	}
	
	/**
	 * @param instancesDecrease - the number of instances to stop when threshold value is crossed.
	 */
	public void setInstancesDecrease(final int instancesDecrease) {
		this.instancesDecrease = instancesDecrease;
	}
	
}
