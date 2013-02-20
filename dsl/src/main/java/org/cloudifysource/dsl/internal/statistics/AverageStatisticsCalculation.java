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

import org.cloudifysource.dsl.statistics.StatisticsCalculation;
import org.openspaces.admin.pu.statistics.AverageInstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.AverageTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;


public class AverageStatisticsCalculation implements StatisticsCalculation {

	/**
	 * Creates a configuration that calculates the average metric value of the specified sliding time window within each service instance. 
	 */
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow,
			TimeUnit timeUnit) {
		return 
			new AverageTimeWindowStatisticsConfigurer()
			.timeWindow(timeWindow, timeUnit)
			.create();
	}

	/**
	 * Creates a configuration that calculates the average metric value across all service instances.
	 */
	@Override
	public InstancesStatisticsConfig createInstancesStatistics() {
		return new AverageInstancesStatisticsConfig();
	}

}
