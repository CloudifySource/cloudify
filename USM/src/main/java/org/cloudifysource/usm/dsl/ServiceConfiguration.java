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
import org.cloudifysource.dsl.internal.context.ServiceContextImpl;

/************
 * DSL based USM configuration.
 * 
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class ServiceConfiguration {

	private final Service service;
	private final File puExtDir;
	private final ServiceContextImpl serviceContext;
	private final File serviceFile;

	public File getServiceFile() {
		return serviceFile;
	}

	public ServiceContextImpl getServiceContext() {
		return serviceContext;
	}

	/************
	 * Constructor.
	 * 
	 * @param service the service POJO.
	 * @param serviceContext the service context.
	 * @param puExtDir the ext dir for the PI instance.
	 * @param serviceFile the DSL dile.
	 */
	public ServiceConfiguration(final Service service, final ServiceContextImpl serviceContext, final File puExtDir,
			final File serviceFile) {
		this.service = service;
		this.serviceContext = serviceContext;
		this.puExtDir = puExtDir;
		this.serviceFile = serviceFile;
	}

	public Service getService() {
		return service;
	}

	public File getPuExtDir() {
		return puExtDir;
	}

}
