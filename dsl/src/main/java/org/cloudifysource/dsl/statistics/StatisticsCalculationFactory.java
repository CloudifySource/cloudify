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
package org.cloudifysource.dsl.statistics;

import org.cloudifysource.dsl.internal.statistics.AverageCpuPercentageAutoScalingStatistics;
import org.cloudifysource.dsl.internal.statistics.AverageStatisticsCalculation;
import org.cloudifysource.dsl.internal.statistics.MaximumStatisticsCalculation;
import org.cloudifysource.dsl.internal.statistics.MinimumStatisticsCalculation;
import org.cloudifysource.dsl.internal.statistics.PercentileStatisticsCalculation;
import org.cloudifysource.dsl.internal.statistics.ThroughputAutoScalingStatistics;


public class StatisticsCalculationFactory {

	/**
	 * supports statistics.average in the recipe (without brackets)
	 */
	public StatisticsCalculation getAverage() {
		return average();
	}
	
	/**
	 * supports statistics.average() in the recipe (with brackets)
	 */
	public StatisticsCalculation average() {
		return new AverageStatisticsCalculation();
	}

	/**
	 * supports statistics.maximum in the recipe (without brackets)
	 */
	public StatisticsCalculation getMaximum() {
		return maximum();
	}
	
	/**
	 * supports statistics.maximum() in the recipe (with brackets)
	 */
	public StatisticsCalculation maximum() {
		return new MaximumStatisticsCalculation();
	}

	/**
	 * supports statistics.minimum() in the recipe (without brackets)
	 */
	public StatisticsCalculation getMinimum() {
		return minimum();
	}
	
	/**
	 * supports statistics.minimum() in the recipe (with brackets)
	 */
	public StatisticsCalculation minimum() {
		return new MinimumStatisticsCalculation();
	}

	/**
	 * supports statistics.percentile(30) in the recipe (with brackets)
	 */
	public StatisticsCalculation percentile(double percentile) {
		return new PercentileStatisticsCalculation(percentile);
	}
	
	/**
	 * supports statistics.median() in the recipe (with brackets)
	 */
	public StatisticsCalculation median() {
		return new PercentileStatisticsCalculation(50.0);
	}
	
	/**
	 * supports statistics.median in the recipe (without brackets)
	 */
	public StatisticsCalculation getMedian() {
		return median();
	}
	
	public TimeWindowStatisticsCalculation throughput() {
		return new ThroughputAutoScalingStatistics();
	}
	
	public TimeWindowStatisticsCalculation getThroughput() {
		return throughput();
	}
	
	public TimeWindowStatisticsCalculation averageCpuPercentage() {
		return new AverageCpuPercentageAutoScalingStatistics();
	}
	
	public TimeWindowStatisticsCalculation getAverageCpuPercentage() {
		return throughput();
	}
		
	public ServiceStatisticsCalculation maximumThroughput() {
		return new ServiceStatisticsCalculation(maximum(), throughput());
	}
	
	public ServiceStatisticsCalculation getMaximumThroughput() {
		return maximumThroughput();
	}
		
	public ServiceStatisticsCalculation minimumThroughput() {
		return new ServiceStatisticsCalculation(minimum(), throughput());
	}
	
	public ServiceStatisticsCalculation getMinimumThroughput() {
		return minimumThroughput();
	}
	
	public ServiceStatisticsCalculation averageThroughput() {
		return new ServiceStatisticsCalculation(average(), throughput());
	}
	
	public ServiceStatisticsCalculation getAverageThroughput() {
		return averageThroughput();
	}

	public ServiceStatisticsCalculation getAverageOfAverages() {
		return averageOfAverages();
	}
	
	public ServiceStatisticsCalculation averageOfAverages() {
		return new ServiceStatisticsCalculation ( average(), average() );
	}
	
	public ServiceStatisticsCalculation getPercentileOfAverages(double percentile) {
		return new ServiceStatisticsCalculation ( percentile(percentile), average() );
	}
	
	public ServiceStatisticsCalculation getMinimumOfAverages() {
		return minimumOfAverages();
	}
	
	public ServiceStatisticsCalculation minimumOfAverages() {
		return new ServiceStatisticsCalculation ( minimum(), average() );
	}
	
	public ServiceStatisticsCalculation getMaximumOfAverages() {
		return maximumOfAverages();
	}
	
	public ServiceStatisticsCalculation maximumOfAverages() {
		return new ServiceStatisticsCalculation ( maximum(), average() );
	}
	
	public ServiceStatisticsCalculation getMaximumOfMaximums() {
		return maximumOfMaximums();
	}
	
	public ServiceStatisticsCalculation maximumOfMaximums() {
		return new ServiceStatisticsCalculation ( maximum(), maximum() );
	}
	
	public ServiceStatisticsCalculation getMinimumOfMinimums() {
		return minimumOfMinimums();
	}
	
	public ServiceStatisticsCalculation minimumOfMinimums() {
		return new ServiceStatisticsCalculation ( minimum(), minimum() );
	}
}
