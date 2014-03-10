/*******************************************************************************
 * Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * 
 * @author adaml
 *  * @since 2.7.1
 *
 */
@JsonRootName("absolute")
public class Limits {
	
	private int maxSecurityGroupRules;
	private int maxSecurityGroups;
	private int maxTotalCores;
	private int maxTotalFloatingIps;
	private int maxTotalInstances;
	private int maxTotalRAMSize;
	
	public int getMaxSecurityGroupRules() {
		return maxSecurityGroupRules;
	}
	
	public void setMaxSecurityGroupRules(final int maxSecurityGroupRules) {
		this.maxSecurityGroupRules = maxSecurityGroupRules;
	}
	
	public int getMaxTotalCores() {
		return maxTotalCores;
	}
	
	public void setMaxTotalCores(final int maxTotalCores) {
		this.maxTotalCores = maxTotalCores;
	}
	
	public int getMaxTotalFloatingIps() {
		return maxTotalFloatingIps;
	}
	
	public void setMaxTotalFloatingIps(final int maxTotalFloatingIps) {
		this.maxTotalFloatingIps = maxTotalFloatingIps;
	}
	
	public int getMaxSecurityGroups() {
		return maxSecurityGroups;
	}
	
	public void setMaxSecurityGroups(final int maxSecurityGroups) {
		this.maxSecurityGroups = maxSecurityGroups;
	}
	
	public int getMaxTotalInstances() {
		return maxTotalInstances;
	}
	
	public void setMaxTotalInstances(final int maxTotalInstances) {
		this.maxTotalInstances = maxTotalInstances;
	}
	
	public int getMaxTotalRAMSize() {
		return maxTotalRAMSize;
	}
	
	public void setMaxTotalRAMSize(final int maxTotalRAMSize) {
		this.maxTotalRAMSize = maxTotalRAMSize;
	}
	
	@Override
	public String toString() {
	 		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
