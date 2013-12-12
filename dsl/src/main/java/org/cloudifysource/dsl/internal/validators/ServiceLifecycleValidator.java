/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal.validators;

import groovy.lang.Closure;

import java.util.Map;

import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.ServiceLifecycle;
import org.cloudifysource.dsl.entry.ClosureExecutableEntry;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 * 
 */
public class ServiceLifecycleValidator implements DSLValidator {

	private ServiceLifecycle entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (ServiceLifecycle) dslEntity;
	}

	@DSLValidation
	void validateStopDetectorIsClosure(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if ((this.entity.getStopDetection() != null)
				&& (!(this.entity.getStopDetection() instanceof ClosureExecutableEntry))) {
			throw new DSLValidationException(
					"The stop detection field only supports execution of closures");
		}
	}

	@DSLValidation
	void validateMonitorsIsClosureOrMap(final DSLValidationContext validationContext)
			throws DSLValidationException {
		Object monitors = this.entity.getMonitors();
		if(monitors == null) {
			return;
		}
		if (monitors instanceof Closure<?>) {
				return;
		}
		
		if(monitors instanceof Map<?, ?>) {
			return;
		}
		
		throw new DSLValidationException(
				"The monitors event only supports execution of closures or returning a map of executable entries");
	}

}
