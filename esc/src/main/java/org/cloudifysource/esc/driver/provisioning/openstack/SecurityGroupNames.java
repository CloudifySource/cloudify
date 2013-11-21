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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.openstack;

/**
 * A class which computes all security group names for a service.
 * 
 * @author victor
 * @since 2.7.0
 */
public class SecurityGroupNames {

	private String prefix;
	private String applicationName;
	private String serviceName;

	public SecurityGroupNames(final String securityGroupPrefix, final String applicationName,
			final String serviceName) {
		this.prefix = securityGroupPrefix;
		this.applicationName = applicationName;
		this.serviceName = serviceName;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getManagementName() {
		return this.prefix + "management";
	}

	public String getAgentName() {
		return this.prefix + "agent";
	}

	public String getClusterName() {
		return this.prefix + "cluster";
	}

	public String getApplicationName() {
		return this.prefix + this.applicationName;
	}

	public String getServiceName() {
		return this.prefix + this.applicationName + "-" + this.serviceName;
	}
}
