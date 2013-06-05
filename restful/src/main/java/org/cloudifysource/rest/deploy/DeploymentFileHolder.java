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
package org.cloudifysource.rest.deploy;

import java.io.File;

/**
 * Deployment file holder.
 * 
 * @author adaml
 *
 */
public class DeploymentFileHolder {
	
	private File packedFile;
	
	private File serviceOverridesFile;
	
	private File applicationPropertiesFile;
	
	public File getPackedFile() {
		return packedFile;
	}
	
	public void setPackedFile(final File packedFile) {
		this.packedFile = packedFile;
	}
	
	public File getServiceOverridesFile() {
		return serviceOverridesFile;
	}
	
	public void setServiceOverridesFile(final File serviceOverridesFile) {
		this.serviceOverridesFile = serviceOverridesFile;
	}
	
	public File getApplicationPropertiesFile() {
		return applicationPropertiesFile;
	}
	
	public void setApplicationPropertiesFile(final File applicationPropertiesFile) {
		this.applicationPropertiesFile = applicationPropertiesFile;
	}
}
