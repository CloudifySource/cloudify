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
package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class ServiceNetworkValidator implements DSLValidator {

	private ServiceNetwork entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (ServiceNetwork) dslEntity;
	}
	
	@DSLValidation
	public void checkPortValue(final DSLValidationContext validationContext)
			throws DSLValidationException {
//		if (this.entity.getPort() <= 0) {
//			throw new DSLValidationException("The port value of the network block must be a positive integer.");
//		}
	}

	@DSLValidation
	public void checkDescription(final DSLValidationContext validationContext)
			throws DSLValidationException {
//		if (this.entity.getProtocolDescription() == null) {
//			throw new DSLValidationException("The protocol description can't be an empty value");
//		}

		
	}

}
