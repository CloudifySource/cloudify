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

import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * This class defines a service deployment which is shared.
 * Each service instance will be allocated according to the requirements specified.
 * @see {@link AppSharedIsolationSLADescriptor}, {@link TenantSharedIsolationSLADescriptor}
 * @author elip
 *
 */
public class SharedIsolationSLADescriptor extends GlobalIsolationSLADescriptor {

	private String isolationId;
	
	public String getIsolationId() {
		return isolationId;
	}

	public void setIsolationId(final String isolationId) {
		this.isolationId = isolationId;
	}
	
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		super.validateDefaultValues(validationContext);
		
		if (isolationId == null) {
			throw new DSLValidationException("isolationId cannot be null");
		}
		if (isUseManagement()) {
			throw new DSLValidationException("isUseManagement can only be true for isolationSLA of type 'global'");
		}
		
	}

	
}
