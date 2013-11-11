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
package org.cloudifysource.esc.driver.provisioning.openstack.rest;

/**
 * @author victor
 * @since 2.7.0
 */
public class UpdatePortRequest extends Port {

	public String computeRequest() {
		final StringBuilder sb = new StringBuilder();

		sb.append("{\"port\":{");

		// security groups
		if (!securityGroupIds.isEmpty()) {
			sb.append("\"security_groups\":[");
			for (String sg : securityGroupIds) {
				sb.append("\"" + sg + "\",");
			}
			sb.setLength(sb.length() - 1);
			sb.append("]");
		}

		sb.append("}}");

		return sb.toString();
	}

}
