/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

	private String subnet;
	private String network;
	private String floatingip;
	@JsonProperty("security_group_rule")
	private String securityGroupRule;
	@JsonProperty("security_group")
	private String securityGroup;
	private String router;
	private String port;
	
	
	public String getSubnet() {
		return subnet;
	}
	
	public void setSubnet(final String subnet) {
		this.subnet = subnet;
	}
	
	public String getNetwork() {
		return network;
	}
	
	public void setNetwork(final String network) {
		this.network = network;
	}
	
	public String getFloatingip() {
		return floatingip;
	}
	
	public void setFloatingip(final String floatingip) {
		this.floatingip = floatingip;
	}
	
	public String getSecurityGroupRule() {
		return securityGroupRule;
	}
	
	public void setSecurityGroupRule(final String securityGroupRule) {
		this.securityGroupRule = securityGroupRule;
	}
	
	public String getSecurityGroup() {
		return securityGroup;
	}
	
	public void setSecurityGroup(final String securityGroup) {
		this.securityGroup = securityGroup;
	}
	
	public String getRouter() {
		return router;
	}
	
	public void setRouter(final String router) {
		this.router = router;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(final String port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
}
