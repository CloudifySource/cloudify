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

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.context.ServiceContext;

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
	private final ServiceContext serviceContext;
	private final File serviceFile;
	private final ClassLoader dslClassLoader;

	public File getServiceFile() {
		return serviceFile;
	}

	public ServiceContext getServiceContext() {
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
	public ServiceConfiguration(final Service service, final ServiceContext serviceContext, final File puExtDir,
			final File serviceFile, final ClassLoader dslClassLoader) {
		this.service = service;
		this.serviceContext = serviceContext;
		this.puExtDir = puExtDir;
		this.serviceFile = serviceFile;
		this.dslClassLoader = dslClassLoader;
	}

	public Service getService() {
		return service;
	}

	public File getPuExtDir() {
		return puExtDir;
	}

	public ClassLoader getDslClassLoader() {
		return dslClassLoader;
	}
	
	@Override
	public String toString() {
		return "ServiceConfiguration [service=" + service + ", serviceContext=" + serviceContext  + ", puExtDir=" 
				+ puExtDir + ", serviceFile=" + serviceFile + ", dslClassLoader=" + dslClassLoader + "]";
	}

}
