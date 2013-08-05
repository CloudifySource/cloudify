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
import org.cloudifysource.domain.statistics.PercentileInstancesStatisticsConfig;

/**
 * 
 * @author adaml
 *
 */
public class PercentileInstancesStatisticsConfigValidaor implements DSLValidator {

	private PercentileInstancesStatisticsConfig entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (PercentileInstancesStatisticsConfig) dslEntity;
	}
	
	@DSLValidation
	public void validate() throws IllegalStateException {

		if (entity.getPercentile() <0 || entity.getPercentile() > 100) {
			throw new IllegalArgumentException("percentile (" + entity.getPercentile() + ") must between 0 and 100 (inclusive)");
		}
	}

}
