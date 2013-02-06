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

package org.cloudifysource.esc.driver.provisioning.jclouds;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;

/*********
 * Interface for cloud drivers that support 're-bootstrapping' - shutting down managers, updating them and restarting
 * the same machines with a new configuration.
 *
 * @author barakme
 * @since 2.5.0
 */
public interface ManagementLocator {

	/**********
	 * Return existing management servers.
	 *
	 * @return the existing management servers, or a 0-length array if non exist.
	 * @throws CloudProvisioningException
	 *             if failed to load servers list from cloud.
	 */
	MachineDetails[] getExistingManagementServers() throws CloudProvisioningException;

}