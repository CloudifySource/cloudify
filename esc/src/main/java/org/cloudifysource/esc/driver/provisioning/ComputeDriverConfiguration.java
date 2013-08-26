/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning;

import org.cloudifysource.domain.cloud.Cloud;
import org.openspaces.admin.Admin;

/************
 * Configuration of a cloud driver instance.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class ComputeDriverConfiguration {

	private Cloud cloud;
	private String cloudTemplate;
	private boolean management;
	private String serviceName;
	private Admin admin;

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public String getCloudTemplate() {
		return cloudTemplate;
	}

	public void setCloudTemplate(final String cloudTemplate) {
		this.cloudTemplate = cloudTemplate;
	}

	public boolean isManagement() {
		return management;
	}

	public void setManagement(final boolean management) {
		this.management = management;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

}
