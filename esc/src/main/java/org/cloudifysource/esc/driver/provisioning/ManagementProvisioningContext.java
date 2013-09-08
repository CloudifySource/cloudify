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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;

/************
 * Provisioning context interface for management provisioning commands.
 * 
 * @author barakme
 * @since 2.6.1
 * 
 */

public interface ManagementProvisioningContext {
	/***********
	 * Creates the Cloudify environment script for management machines.
	 * 
	 * @param mds
	 *            the management machine details.
	 * @param template
	 *            the template to use for these management machine.
	 * @return the contents of each of the cloudify environment files, one per management server, in the same order as
	 *         the machine details that were provided..
	 * @throws FileNotFoundException
	 *             If a provided key file does not exist.
	 */
	String[] createManagementEnvironmentScript(final MachineDetails[] mds,
			final ComputeTemplate template) throws FileNotFoundException;

	
	/***************
	 * Returns the directory where the cloud configuration was read. 
	 * @return the cloud configuration directory.
	 */
	File getCloudFile();
}
