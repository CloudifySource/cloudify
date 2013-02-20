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
package org.cloudifysource.dsl;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/****************
 * Domain POJO for an Application recipe.
 * @author barakme
 * @since 2.0.0
 *
 */
@CloudifyDSLEntity(name = "application", clazz = Application.class, allowInternalNode = false, allowRootNode = true)
public class Application {

	private String name;

	private List<Service> services = new LinkedList<Service>();

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<Service> getServices() {
		return services;
	}

	public void setServices(final List<Service> services) {
		this.services = services;
	}

	@Override
	public String toString() {
		return "Application [name=" + name + ", services=" + services + "]";
	}

	// This is a hack, but it allows the application DSL to work with the existing DSL base script.
	/***
	 * .
	 * @param service .
	 */
	public void setService(final Service service) {
		this.services.add(service);
	}

	/****
	 * .
	 * @return .
	 */
	public Service getService() {
		if (this.getServices().isEmpty()) {
			return null;
		}
		return this.services.get(this.services.size() - 1);
	}
	
	/**
	 * Validates that the name property exists and is not empty or invalid.
	 * @param validationContext
	 * @throws DSLValidationException
	 */
	@DSLValidation
	void validateName(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		if (StringUtils.isBlank(name)) {
			throw new DSLValidationException("Application.validateName: The application's name " 
					+ (name == null ? "is missing" : "is empty"));
		}
		
		DSLUtils.validateRecipeName(name);
	}

}
