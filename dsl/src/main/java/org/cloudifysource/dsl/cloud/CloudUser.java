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
package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/********************
 * Domain POJO for the cloud user.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
@CloudifyDSLEntity(name = "user", clazz = CloudUser.class, allowInternalNode = true, allowRootNode = false,
		parent = "cloud")
public class CloudUser {

	private String user;
	private String apiKey;
	

	public String getUser() {
		return user;
	}

	public void setUser(final String user) {
		this.user = user;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	
	@Override
	public String toString() {
		return "CloudUser [user=" + user + "]";
	}

	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if ("ENTER_USER".equals(this.getUser())) {
			throw new DSLValidationException("User field still has default configuration value of ENTER_USER");
		}

		if ("ENTER_KEY".equals(this.getApiKey())) {
			throw new DSLValidationException("Key field still has default configuration value of ENTER_KEY");
		}

	}
}
