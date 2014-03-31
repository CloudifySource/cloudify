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
import org.cloudifysource.domain.context.network.NetworkFacade;

/**
 * Base service context for services running in localcloud.
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
		unsupported();
		return null;
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
		unsupported();
		return null;
	}

	@Override
	public long getExternalProcessId() {
		unsupported();
		return 0l;
	}

	@Override
	public boolean isLocalCloud() {
		// TODO: this should throw an unsupported exception. 
		return true;
	}

	@Override
	public String getPublicAddress() {
		unsupported();
		return null;
	}

	@Override
	public String getPrivateAddress() {
		unsupported();
		return null;
	}

	@Override
	public String getImageID() {
		unsupported();
		return null;
	}

	@Override
	public String getHardwareID() {
		unsupported();
		return null;
	}

	@Override
	public String getCloudTemplateName() {
		unsupported();
		return null;
	}

	@Override
	public String getMachineID() {
		unsupported();
		return null;
	}

	@Override
	public String getLocationId() {
		unsupported();
		return null;
	}

	@Override
	public StorageFacade getStorage() {
		unsupported();
		return null;
	}

	@Override
	public boolean isPrivileged() {
		unsupported();
		return false;
	}

	@Override
	public String getBindAddress() {
		unsupported();
		return null;
	}

	@Override
	public AttributesFacade getAttributes() {
		unsupported();
		return null;
	}

	@Override
	public void stopMaintenanceMode() {
		unsupported();
	}

	@Override
	public void startMaintenanceMode(final long timeout, final TimeUnit unit) {
		unsupported();
	}

	@Override
	public NetworkFacade getNetwork() {
		unsupported();
		return null;
	}
	
	private void unsupported() {
		throw new UnsupportedOperationException("context method is not supported for" 
				+ " services running outside a GSC.");
	}

	@Override
	public String getAttributesStoreDiscoveryTimeout() {
		unsupported();
		return null;
	}

}
