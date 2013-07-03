package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.statistics.PercentileInstancesStatisticsConfig;

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
