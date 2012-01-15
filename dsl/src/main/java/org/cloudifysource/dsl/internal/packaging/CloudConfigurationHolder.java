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
package org.cloudifysource.dsl.internal.packaging;

import com.gigaspaces.annotation.pojo.SpaceId;


public class CloudConfigurationHolder {

	private String cloudConfiguration;
	private Long id = (long) 1;
	
	public CloudConfigurationHolder() {
		
	}
	
	public CloudConfigurationHolder(final String cloudConfigution) {
		this.cloudConfiguration = cloudConfigution;		
	}
	
	@SpaceId
	public Long getId() { 
		return id;
	}
	
	public void setId(final Long id) {
		this.id = id;
	}
	public String getCloudConfiguration() {
		return this.cloudConfiguration;
	}
	
	public void setCloudConfiguration(final String cloudConfiguration) { 
		this.cloudConfiguration = cloudConfiguration;
	}
	
}
