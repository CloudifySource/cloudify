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

/**
 * 
 * A helper DSL factory method for defining statistics calculations.
 * @see {@link org.cloudifysource.dsl.internal.DSLReader#createCompilerConfiguration} for the "Statistics" static import
 * 
 * @author itaif
 * @since 2.1
 *
 */
public class StatisticsCalculationFactory {

	/**
	 * Reduces metric series to the average value.
	 * Applies for time series or instances series.
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation getAverage() {
		return average();
	}
	
	/**
	 * Reduces metric series to the average value.
	 * Applies for time series or instances series.
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics() 
	 */
	public StatisticsCalculation average() {
		return new AverageStatisticsCalculation();
	}

	/**
	 * Reduces metric series to the maximum value.
	 * Applies for time series or instances series.
	 * @see MaximumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation getMaximum() {
		return maximum();
	}
	
	/**
	 * Reduces metric series to the maximum value.
	 * Applies for time series or instances series.
	 * @see MaximumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation maximum() {
		return new MaximumStatisticsCalculation();
	}

	/**
	 * Reduces metric series to the minimum value.
	 * Applies for time series or instances series.
	 * @see MinimumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation getMinimum() {
		return minimum();
	}
	
	/**
	 * Reduces metric series to the minimum value.
	 * Applies for time series or instances series.
	 * @see MinimumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation minimum() {
		return new MinimumStatisticsCalculation();
	}

	/**
	 * Reduces metric series to the specified percentile value.
	 * Applies for time series or instances series.
	 * For example if percentile is 50, will choose the median value.
	 * @see PercentileStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see PercentileStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation percentile(double percentile) {
		return new PercentileStatisticsCalculation(percentile);
	}
	
	/**
	 * Reduces metric series to the median value. Same as percentile(50)
	 * Applies for time series or instances series.
	 * @see PercentileStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see PercentileStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation median() {
		return new PercentileStatisticsCalculation(50.0);
	}
	
	/**
	 * Reduces metric series to the median value.
	 * Applies for time series or instances series.
	 * @see PercentileStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see PercentileStatisticsCalculation#createInstancesStatistics()
	 */
	public StatisticsCalculation getMedian() {
		return median();
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to throughput.
	 * Applies for time series.
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 */
	public TimeWindowStatisticsCalculation throughput() {
		return new ThroughputAutoScalingStatistics();
	}

	/**
	 * Reduces metric time series of total requests per seconds to throughput.
	 * Applies for time series.
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 */
	public TimeWindowStatisticsCalculation getThroughput() {
		return throughput();
	}
	
	/**
	 * Reduces metric time series of total CPU ticks, to CPU percentage.
	 * Applies for time series.
	 * @see AverageCpuPercentageAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 */
	public TimeWindowStatisticsCalculation averageCpuPercentage() {
		return new AverageCpuPercentageAutoScalingStatistics();
	}
	
	/**
	 * Reduces metric time series of total CPU ticks, to CPU percentage.
	 * Applies for time series.
	 * @see AverageCpuPercentageAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 */
	public TimeWindowStatisticsCalculation getAverageCpuPercentage() {
		return throughput();
	}

	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then chooses the maximum throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation maximumThroughput() {
		return new ServiceStatisticsCalculation(maximum(), throughput());
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then chooses the maximum throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMaximumThroughput() {
		return maximumThroughput();
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then chooses the minimum throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation minimumThroughput() {
		return new ServiceStatisticsCalculation(minimum(), throughput());
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then chooses the minimum throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMinimumThroughput() {
		return minimumThroughput();
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then calculates the average throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation averageThroughput() {
		return new ServiceStatisticsCalculation(average(), throughput());
	}
	
	/**
	 * Reduces metric time series of total requests per seconds to per-instance throughput
	 * then calculates the average throughput among instances.  
	 * @see ThroughputAutoScalingStatistics#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getAverageThroughput() {
		return averageThroughput();
	}

	/**
	 * Reduces metric time series to the average value per-instance
	 * then calculates the average value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getAverageOfAverages() {
		return averageOfAverages();
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then calculates the average value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see AverageStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation averageOfAverages() {
		return new ServiceStatisticsCalculation ( average(), average() );
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then chooses the percentile value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see PercentileStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getPercentileOfAverages(double percentile) {
		return new ServiceStatisticsCalculation ( percentile(percentile), average() );
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then chooses the minimum value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMinimumOfAverages() {
		return minimumOfAverages();
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then chooses the minimum value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation minimumOfAverages() {
		return new ServiceStatisticsCalculation ( minimum(), average() );
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then chooses the maximum value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMaximumOfAverages() {
		return maximumOfAverages();
	}
	
	/**
	 * Reduces metric time series to the average value per-instance
	 * then chooses the maximum value among instances.  
	 * @see AverageStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation maximumOfAverages() {
		return new ServiceStatisticsCalculation ( maximum(), average() );
	}
	
	/**
	 * Reduces metric time series to the maximum value per-instance
	 * then chooses the maximum value among instances.  
	 * @see MaximumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMaximumOfMaximums() {
		return maximumOfMaximums();
	}
	
	/**
	 * Reduces metric time series to the maximum value per-instance
	 * then chooses the maximum value among instances.  
	 * @see MaximumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MaximumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation maximumOfMaximums() {
		return new ServiceStatisticsCalculation ( maximum(), maximum() );
	}
	
	/**
	 * Reduces metric time series to the minimum value per-instance
	 * then chooses the minimum value among instances.  
	 * @see MinimumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation getMinimumOfMinimums() {
		return minimumOfMinimums();
	}
	
	/**
	 * Reduces metric time series to the minimum value per-instance
	 * then chooses the minimum value among instances.  
	 * @see MinimumStatisticsCalculation#createTimeWindowStatistics(long, java.util.concurrent.TimeUnit)
	 * @see MinimumStatisticsCalculation#createInstancesStatistics()
	 */
	public ServiceStatisticsCalculation minimumOfMinimums() {
		return new ServiceStatisticsCalculation ( minimum(), minimum() );
	}
}
