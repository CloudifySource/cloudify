/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.domain.context;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.context.blockstorage.StorageFacade;
import org.cloudifysource.domain.context.kvstorage.AttributesFacade;
import org.cloudifysource.domain.context.network.NetworkProvisioningDriver;

/**
 * 
 * @author adaml
 *
 */
public class BaseServiceContext implements ServiceContext {

	private Service service;
	private String serviceDirectory;
	
	/**
	 * init the base service context
	 *  
	 * @param service
	 * 		The installed service
	 */
	public void init(final Service service) {
		this.service = service;
	}
	
	public BaseServiceContext(final String serviceDirectory) {
		this.serviceDirectory = serviceDirectory;
	}

	@Override
	public int getInstanceId() {
		return 1;
	}

	@Override
	public org.cloudifysource.domain.context.Service waitForService(final String name, 
																final int timeout, 
																final TimeUnit unit) {
		throw new UnsupportedOperationException("waitForService context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getServiceDirectory() {
		return this.serviceDirectory;
	}

	@Override
	public String getServiceName() {
		return service.getName();
	}

	@Override
	public String getApplicationName() {
		throw new UnsupportedOperationException("getApplicationName context method is not supported for" 
				+ " services running outside a GSC.");
	}

	@Override
	public long getExternalProcessId() {
		throw new UnsupportedOperationException("getExternalProcessId context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public boolean isLocalCloud() {
		return true;
	}

	@Override
	public String getPublicAddress() {
		throw new UnsupportedOperationException("getPublicAddress context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getPrivateAddress() {
		throw new UnsupportedOperationException("getPrivateAddress context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getImageID() {
		throw new UnsupportedOperationException("getImageID context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getHardwareID() {
		throw new UnsupportedOperationException("getHardwareID context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getCloudTemplateName() {
		throw new UnsupportedOperationException("getCloudTemplateName context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getMachineID() {
		throw new UnsupportedOperationException("getMachineID context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getLocationId() {
		throw new UnsupportedOperationException("getLocationId context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public StorageFacade getStorage() {
		throw new UnsupportedOperationException("getStorage context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public boolean isPrivileged() {
		throw new UnsupportedOperationException("isPrivileged context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public String getBindAddress() {
		throw new UnsupportedOperationException("getBindAddress context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public AttributesFacade getAttributes() {
		throw new UnsupportedOperationException("getAttributes context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public void stopMaintenanceMode() {
		throw new UnsupportedOperationException("stopMaintenanceMode context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public void startMaintenanceMode(final long timeout, final TimeUnit unit) {
		throw new UnsupportedOperationException("startMaintenanceMode context method is not supported for"
				+ " services running outside a GSC.");
	}

	@Override
	public NetworkProvisioningDriver getNetwork() {
		throw new UnsupportedOperationException("getNetwork context method is not supported for"
				+ " services running outside a GSC.");
	}

}
