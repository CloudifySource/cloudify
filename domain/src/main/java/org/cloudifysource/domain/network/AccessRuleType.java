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

/*************
 * Enum for service access rule types.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public enum AccessRuleType {
	/************
	 * Access matching this rule will be blocked.
	 */
	PRIVATE,
	/***********************
	 * Access allowed for instances from the same service.
	 */
	SERVICE,
	/***********
	 * Access allowed for instances from the same application.
	 */
	APPLICATION,
	/************
	 * Access allowed for all instances in this Cloudify cluster.
	 */
	CLUSTER,
	/********
	 * Access always allowed.
	 */
	PUBLIC,
	/********
	 * Access allowed from a specific IP range, defined as a CIDR block.
	 */
	RANGE,

	/***********
	 * Access allowed from a specific, cloud-defined, group. Commonly a security group, though specific network drivers
	 * may override this).
	 */
	GROUP

}
