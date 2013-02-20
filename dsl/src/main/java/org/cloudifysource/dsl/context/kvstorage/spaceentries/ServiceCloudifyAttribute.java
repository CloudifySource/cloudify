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
package org.cloudifysource.dsl.context.kvstorage.spaceentries;

/**
 * Service level property.
 * 
 * @author eitany
 * @since 2.0
 */
public class ServiceCloudifyAttribute extends AbstractCloudifyAttribute {

	public ServiceCloudifyAttribute() {
	}

	public ServiceCloudifyAttribute(final String applicationName, final String serviceName, final String key,
			final Object value) {
		super(applicationName, key, value);
		this.serviceName = serviceName;
	}

	private String serviceName;

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

}
