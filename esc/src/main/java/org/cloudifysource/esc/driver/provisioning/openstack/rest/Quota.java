/*******************************************************************************
 * Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * @since 2.7.1
 * @author noak
 */
@JsonRootName("quota")
public class Quota {

	private int subnet;
	private int network;
	private int floatingip;
	@JsonProperty("security_group_rule")
	private int securityGroupRule;
	@JsonProperty("security_group")
	private int securityGroup;
	private int router;
	private int port;
	
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}


	public int getSubnet() {
		return subnet;
	}


	public void setSubnet(final int subnet) {
		this.subnet = subnet;
	}


	public int getNetwork() {
		return network;
	}


	public void setNetwork(final int network) {
		this.network = network;
	}


	public int getFloatingip() {
		return floatingip;
	}


	public void setFloatingip(final int floatingip) {
		this.floatingip = floatingip;
	}


	public int getSecurityGroupRule() {
		return securityGroupRule;
	}


	public void setSecurityGroupRule(final int securityGroupRule) {
		this.securityGroupRule = securityGroupRule;
	}


	public int getSecurityGroup() {
		return securityGroup;
	}


	public void setSecurityGroup(final int securityGroup) {
		this.securityGroup = securityGroup;
	}


	public int getRouter() {
		return router;
	}


	public void setRouter(final int router) {
		this.router = router;
	}


	public int getPort() {
		return port;
	}


	public void setPort(final int port) {
		this.port = port;
	}

}
