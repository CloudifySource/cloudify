/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.scalingrules;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Domain Object for defining SLA based on statistics and thresholds that triggers 
 * scale out or scale in action.
 * 
 * @author itaif
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */
@CloudifyDSLEntity(name = "scalingRule", clazz = ScalingRuleDetails.class, 
	allowInternalNode = true, allowRootNode = true, parent = "service")
public class ScalingRuleDetails {

	private Object serviceStatistics;
	
	private HighThresholdDetails highThreshold;
	
	private LowThresholdDetails lowThreshold;

	public HighThresholdDetails getHighThreshold() {
		return highThreshold;
	}

	/**
	 * @param highThreshold
	 * 			a wrapper object for the breach value and the increase action value
	 */
	public void setHighThreshold(final HighThresholdDetails highThreshold) {
		this.highThreshold = highThreshold;
	}

	public LowThresholdDetails getLowThreshold() {
		return lowThreshold;
	}

	/**
	 * @param lowThreshold
	 * 			a wrapper object for the breach value and the decrease action value
	 */
	public void setLowThreshold(final LowThresholdDetails lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public Object getServiceStatistics() {
		return serviceStatistics;
	}

	/**
	 * @param serviceStatistics - The statistics name to compare the threshold against. 
	 * This could either be a string referencing a predefined serviceStatistics or a serviceStatistics closure
	 */
	public void setServiceStatistics(final Object serviceStatistics) {
		this.serviceStatistics = serviceStatistics;
	}
}
