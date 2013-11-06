/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

import java.io.File;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.context.ServiceContext;

/*******
 * Result of reading a Service DSL file.
 * @author barakme
 *
 */
public class DSLServiceCompilationResult {
	private final Service service;
	private final Cloud cloud;
	private final File servicePropertiesFile;
	private final File serviceOverridesFile;
	private final ServiceContext context;
	private final File dslFile;
	
	public DSLServiceCompilationResult(final Service service, final ServiceContext context, final Cloud cloud,
			final File dslFile, final File servicePropertiesFile, final File serviceOverridesFile) {
		super();
		this.service = service;
		this.context = context;
		this.dslFile = dslFile;
		this.cloud = cloud;
		this.servicePropertiesFile = servicePropertiesFile;
		this.serviceOverridesFile = serviceOverridesFile;
	}

	public DSLServiceCompilationResult(final Service service, final ServiceContext context,
			final File dslFile) {
		this(service, context, null, dslFile, null, null);
	}

	public Service getService() {
		return service;
	}

	public ServiceContext getContext() {
		return context;
	}

	public File getDslFile() {
		return dslFile;
	}

	public final File getServicePropertiesFile() {
		return servicePropertiesFile;
	}

	public final File getServiceOverridesFile() {
		return serviceOverridesFile;
	}
	
	public Cloud getCloud() {
		return cloud;
	}
}