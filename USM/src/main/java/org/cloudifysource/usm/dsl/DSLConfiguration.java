/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.usm.dsl;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.usm.CommandParts;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;




public class DSLConfiguration implements UniversalServiceManagerConfiguration {

	private final Service service;
	private final File puExtDir;
	private final ServiceContext serviceContext;
	private final File serviceFile;

	public File getServiceFile() {
		return serviceFile;
	}

	public ServiceContext getServiceContext() {
		return serviceContext;
	}

	public DSLConfiguration(final Service service, ServiceContext serviceContext, final File puExtDir, final File serviceFile) {
		this.service = service;
		this.serviceContext = serviceContext;
		this.puExtDir = puExtDir;
		this.serviceFile = serviceFile;
	}
	
	@Override
	public Object getStartCommand() {
		final Object start = this.service.getLifecycle().getStart();

		return start;

	}

	@Override
	public int getNumberOfLaunchRetries() {
		return 0;
	}

	@Override
	public String getPidFile() {
		return null;
	}

	public CommandParts getWindowsCommandParts() {
		return null;
	}

	public CommandParts getLinuxCommandParts() {
		return null;
	}

	public Service getService() {
		return service;
	}

	public File getPuExtDir() {
		return puExtDir;
	}

	@Override
	public String getServiceName() {
		return this.service.getName();
	}

	@Override
	public long getStartDetectionTimeoutMSecs() {
		return this.service.getLifecycle().getStartDetectionTimeoutSecs() * 1000;
	}

	@Override
	public long getStartDetectionIntervalMSecs() {
		return this.service.getLifecycle().getStartDetectionIntervalSecs() * 1000;
	}

}
