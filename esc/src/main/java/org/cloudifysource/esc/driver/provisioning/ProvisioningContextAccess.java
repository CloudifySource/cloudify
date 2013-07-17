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

/****************
 * Provides access to the provisioning context of the currently running provisioning request.
 * 
 * @author barakme
 * @since 2.6.1
 * 
 */
public class ProvisioningContextAccess {

	private static ThreadLocal<ProvisioningContext> contextHolder = new ThreadLocal<ProvisioningContext>();
	private static ThreadLocal<ManagementProvisioningContext> mgtContextHolder =
			new ThreadLocal<ManagementProvisioningContext>();

	public ProvisioningContextAccess() {

	}

	public ProvisioningContext getProvisioiningContext() {
		return contextHolder.get();
	}

	/********
	 * Sets the current provisioning context.
	 * @param context .
	 */
	public static void setCurrentProvisioingContext(final ProvisioningContext context) {
		contextHolder.set(context);
	}

	public ManagementProvisioningContext getManagementProvisioiningContext() {
		return mgtContextHolder.get();
	}

	/******
	 * Sets the current management provisioning context.
	 * @param context .
	 */
	public static void setCurrentManagementProvisioingContext(final ManagementProvisioningContext context) {
		mgtContextHolder.set(context);
	}

}
