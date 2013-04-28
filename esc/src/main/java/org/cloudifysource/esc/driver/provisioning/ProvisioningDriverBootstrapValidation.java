/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning;

import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;

/*****************
 * This interface defines the methods required for cloud-specific validation.
 * It must be implemented by cloud drivers in order to support validations as part of the bootstrapping process.
 * 
 * @author noak
 *
 */
public interface ProvisioningDriverBootstrapValidation {

	
	/**
	 * Cloud-specific validations called after setConfig and before machines are allocated.
	 * 
	 * @param validationContext
	 *            The object through which writing of validation messages is done
	 * @throws CloudProvisioningException Indicates invalid configuration
	 */
	void validateCloudConfiguration(final ValidationContext validationContext) throws CloudProvisioningException;
}
