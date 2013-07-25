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
package org.cloudifysource.dsl.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.statistics.InstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.MinimumInstancesStatisticsConfig;
import org.cloudifysource.dsl.statistics.MinimumTimeWindowStatisticsConfig;
import org.cloudifysource.dsl.statistics.StatisticsCalculation;
import org.cloudifysource.dsl.statistics.TimeWindowStatisticsConfig;

public class MinimumStatisticsCalculation implements StatisticsCalculation {

	/**
	 * Creates a configuration that chooses the minimum metric 
	 * from the specified sliding time window within each service instance.
	 */
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		MinimumTimeWindowStatisticsConfig config = new MinimumTimeWindowStatisticsConfig();
		config.setTimeWindowSeconds(timeUnit.toSeconds(timeWindow));
		return config;
	}

	/**
	 * Creates a configuration that chooses the minimum metric value across service instances.
	 */
	@Override
	public InstancesStatisticsConfig createInstancesStatistics() {
		return new MinimumInstancesStatisticsConfig();
	}
}
