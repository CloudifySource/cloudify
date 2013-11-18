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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;

/**************************************************
 * Context for agent provisioning request.
 * 
 * @author barakme
 * @since 2.6.1
 * 
 */
public interface ProvisioningContext {

	/***********
	 * Creates the provisioning environment script file contents for this provisioing request.
	 * 
	 * @param md
	 *            the created machine node.
	 * @param template
	 *            the template used with this node.
	 * @return the contents of the cloudify environment file which should be executed before the bootstrap script runs.
	 * @throws FileNotFoundException
	 *             if a key file specified in the machine details does not exist.
	 */
	String createEnvironmentScript(final MachineDetails md, final ComputeTemplate template)
			throws FileNotFoundException;

	/***************
	 * Returns the directory where the cloud configuration was read. 
	 * @return the cloud configuration directory.
	 */
	File getCloudFile();

	/*********
	 * The location ID of the current provisioning request.
	 * @return the location ID.
	 */
	String getLocationId();
	
	/***************************
	 * If this provisioning request is needed to recover from the previous failure of another machine, the previous machine's details are available here. Otherwise null.
	 * 
	 * @return the machine details of the previous failed machine that the new machine will replace.   
	 */
	MachineDetails getPreviousMachineDetails();
		
	

}
