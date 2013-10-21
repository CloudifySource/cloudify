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

import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.network.AccessRule;
import org.cloudifysource.domain.network.AccessRuleType;
import org.cloudifysource.domain.network.PortRangeFactory;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class AccessRuleValidator implements DSLValidator {

	private AccessRule entity;

	@Override
	public void setDSLEntity(final Object dslEntity) {
		this.entity = (AccessRule) dslEntity;
	}

	@DSLValidation
	public void checkDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getType() == null) {
			throw new DSLValidationException("Missing type in access rule");
		}
		if (this.entity.getType().equals(AccessRuleType.GROUP)
				&& this.entity.getTarget() == null) {
			throw new DSLValidationException("Missing target in access rule of type GROUP");
		}
		if (this.entity.getType().equals(AccessRuleType.RANGE)
				&& this.entity.getTarget() == null) {
			throw new DSLValidationException("Missing target in access rule of type RANGE");
		}

	}

	@DSLValidation
	public void checkPortRange(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getPortRange() == null) {
			throw new DSLValidationException("Missing port range in access rule");
		}
		final String range = this.entity.getPortRange();

		try {
			PortRangeFactory.createPortRange(range);
		} catch (final IllegalArgumentException e) {
			throw new DSLValidationException("Invalid port range description: " + e.getMessage(), e);
		}

	}
}
