package org.cloudifysource.dsl.scalingrules;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Domain Object for defining SLA based on thresholds that triggers 
 * scale out action.
 * 
 * @author elip
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */

@CloudifyDSLEntity(name = "highThreshold" , clazz = HighThresholdDetails.class , 
		allowInternalNode = true , allowRootNode = false , parent = "scalingRule")
public class HighThresholdDetails {

	private Comparable<?> value;
	private int instancesIncrease;
	
	public Comparable<?> getValue() {
		return value;
	}

	/**
	 * @param value - the threshold value above which number of instances is increased.
	 */
	public void setValue(final Comparable<?> value) {
		this.value = value;
	}
	
	public int getInstancesIncrease() {
		return instancesIncrease;
	}
	
	/**
	 * @param instancesIncrease - the number of instances to start when threshold value is crossed.
	 */
	public void setInstancesIncrease(final int instancesIncrease) {
		this.instancesIncrease = instancesIncrease;
	}

}
