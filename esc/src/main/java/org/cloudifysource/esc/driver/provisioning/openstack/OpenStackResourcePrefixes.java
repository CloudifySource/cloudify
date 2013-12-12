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
 * A class which computes prefixes for all openstack resources, especially for security groups and networks names.
 * 
 * @author victor
 * @since 2.7.0
 */
public class OpenStackResourcePrefixes {

	private String prefix;
	private String applicationName;
	private String serviceName;

	public OpenStackResourcePrefixes(final String securityGroupPrefix, final String applicationName,
			final String serviceName) {
		this.prefix = securityGroupPrefix;
		this.applicationName = applicationName;
		this.serviceName = serviceName;
	}

	/**
	 * All created Openstack resources should be prefixed.<br />
	 * The prefix is the management group name defined in the cloud DSL.
	 * 
	 * @return The prefix for all created openstack resources.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Get a prefix name for management group.<br />
	 * The prefix convention is : managementGroupPrefix + "management".
	 * 
	 * @return A prefix name for management group.
	 */
	public String getManagementName() {
		return this.prefix + "management";
	}

	/**
	 * Get a prefix name for agent group.<br />
	 * The prefix convention is : managementGroupPrefix + "agent".
	 * 
	 * @return A prefix name for agent group.
	 */
	public String getAgentName() {
		return this.prefix + "agent";
	}

	/**
	 * Get a prefix name for cluster group.<br />
	 * The prefix convention is : managementGroupPrefix + "cluster".
	 * 
	 * @return A prefix name for cluster group.
	 */
	public String getClusterName() {
		return this.prefix + "cluster";
	}

	/**
	 * Get a prefix name for applications group.<br />
	 * The prefix convention is : managementGroupPrefix + applicationName.<br />
	 * i.e.: cloudify-management-myApplication
	 * 
	 * @return A prefix name for applications group.
	 */
	public String getApplicationName() {
		return this.prefix + this.applicationName;
	}

	/**
	 * Get a prefix name for services group.<br />
	 * The prefix convention is : managementGroupPrefix + applicationName + "-" + serviceName.<br />
	 * i.e.: cloudify-management-myApplication-myService
	 * 
	 * @return A prefix name for services group.
	 */
	public String getServiceName() {
		return this.prefix + this.applicationName + "-" + this.serviceName;
	}

}
