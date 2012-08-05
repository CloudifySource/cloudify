/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.client;

/*******************************************
 * A POJO holding the Deployment details.  * 
 * @author elip							   *
 *										   *
 *******************************************/
public class DeletePersistentRoleVMDeploymentDetails {
	
	private String deploymentName;
	private String storageAccountName;
	private String hostedServiceName;
	private String osDisk;
	private String roleName;
	
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}

	public String getDeploymentName() {
		return deploymentName;
	}
	
	public void setDeploymentName(final String deploymentName) {
		this.deploymentName = deploymentName;
	}
	
	public String getStorageAccountName() {
		return storageAccountName;
	}
	
	public void setStorageAccountName(final String storageAccountName) {
		this.storageAccountName = storageAccountName;
	}
	
	public String getHostedServiceName() {
		return hostedServiceName;
	}
	
	public void setHostedServiceName(final String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}
	
	public String getOsDisk() {
		return osDisk;
	}
	
	public void setOsDisk(final String osDisk) {
		this.osDisk = osDisk;
	}	
}
