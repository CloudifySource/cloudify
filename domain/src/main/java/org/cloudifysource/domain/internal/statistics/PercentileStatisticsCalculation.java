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
package org.cloudifysource.domain.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.statistics.InstancesStatisticsConfig;
import org.cloudifysource.domain.statistics.PercentileInstancesStatisticsConfig;
import org.cloudifysource.domain.statistics.PercentileTimeWindowStatisticsConfig;
import org.cloudifysource.domain.statistics.StatisticsCalculation;
import org.cloudifysource.domain.statistics.TimeWindowStatisticsConfig;

public class PercentileStatisticsCalculation implements StatisticsCalculation {

	private final double percentile;

	public PercentileStatisticsCalculation(double percentile) {
		this.percentile = percentile;
	}

	public double getPercentile() {
		return percentile;
	}
	
	/**
	 * Creates a configuration that chooses the percentile metric value of the specified sliding time window within each service instance.
	 * For example, if percentile is 50, it chooses the median metric value of the specified time window.  
	 */
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		PercentileTimeWindowStatisticsConfig config = new PercentileTimeWindowStatisticsConfig();
		config.setPercentile(percentile);
		config.setTimeWindowSeconds(timeUnit.toSeconds(timeWindow));
		return config;
	}

	/**
	 * Creates a configuration that chooses the percentile metric value of all service instances.
	 * For example, if percentile is 50, it chooses the median metric value of all service instances.  
	 */
	@Override
	public InstancesStatisticsConfig createInstancesStatistics() {
		PercentileInstancesStatisticsConfig config = new PercentileInstancesStatisticsConfig();
		config.setPercentile(percentile);
		return config;
	}

}
