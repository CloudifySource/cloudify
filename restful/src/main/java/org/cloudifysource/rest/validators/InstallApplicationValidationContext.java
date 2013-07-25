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
package org.cloudifysource.rest.validators;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.cloud.Cloud;

/**
 * 
 * install application validation context containing all necessary 
 * validation parameters required for validation.
 * 
 * @author adaml
 *
 */
public class InstallApplicationValidationContext {

	private Cloud cloud;
	
	private Application application;

	public Cloud getCloud() {
		return this.cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(final Application application) {
		this.application = application;
	}
}
