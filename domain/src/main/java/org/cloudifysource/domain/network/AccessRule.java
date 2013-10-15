/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.domain.network;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/******
 * A network access rule for a Cloudify service.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
@CloudifyDSLEntity(name = "rule", clazz = AccessRule.class,
		allowInternalNode = true, allowRootNode = true, parent = "accessRules")
public class AccessRule {

	private AccessRuleType type = null;
	private String portRange = null;
	private String target = null;

	public AccessRule() {

	}

	public AccessRuleType getType() {
		return type;
	}

	public void setType(final AccessRuleType type) {
		this.type = type;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

	/********
	 * A port range to which this rule applied. Port ranges are in the format: PORT_RANGE= RANGE,RANGE RANGE = N | N-M N
	 * = valid port number. Note that for ranges defined as N-M, M must be larger then, or equal to, N.
	 * 
	 * 
	 * Example: 80 22,80 8099-8100 22,80,8099-8100,8080-8090
	 * 
	 * @return the port range.
	 */
	public String getPortRange() {
		return portRange;
	}

	public void setPortRange(final String portRange) {
		this.portRange = portRange;
	}

}
