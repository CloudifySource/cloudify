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
package org.cloudifysource.dsl.autoscaling;

public class AutoScalingStatisticsFactory {

	/**
	 * supports statistics.average in the recipe (without brackets)
	 */
	public AverageAutoScalingStatistics getAverage() {
		return average();
	}
	
	/**
	 * supports statistics.average() in the recipe (with brackets)
	 */
	public AverageAutoScalingStatistics average() {
		return new AverageAutoScalingStatistics();
	}

	/**
	 * supports statistics.maximum in the recipe (without brackets)
	 */
	public MaximumAutoScalingStatistics getMaximum() {
		return maximum();
	}
	
	/**
	 * supports statistics.maximum() in the recipe (with brackets)
	 */
	public MaximumAutoScalingStatistics maximum() {
		return new MaximumAutoScalingStatistics();
	}

	/**
	 * supports statistics.minimum() in the recipe (without brackets)
	 */
	public MinimumAutoScalingStatistics getMinimum() {
		return minimum();
	}
	
	/**
	 * supports statistics.minimum() in the recipe (with brackets)
	 */
	public MinimumAutoScalingStatistics minimum() {
		return new MinimumAutoScalingStatistics();
	}

	/**
	 * supports statistics.percentile(30) in the recipe (with brackets)
	 */
	public PercentileAutoScalingStatistics percentile(double percentile) {
		return new PercentileAutoScalingStatistics(percentile);
	}
	
	/**
	 * supports statistics.median() in the recipe (with brackets)
	 */
	public PercentileAutoScalingStatistics median() {
		return new PercentileAutoScalingStatistics(50.0);
	}
	
	/**
	 * supports statistics.median in the recipe (without brackets)
	 */
	public PercentileAutoScalingStatistics getMedian() {
		return median();
	}
	
	public ThroughputAutoScalingStatistics throughput() {
		return new ThroughputAutoScalingStatistics();
	}
	
	public ThroughputAutoScalingStatistics getThroughput() {
		return throughput();
	}
		
	public AutoScalingStatisticsPair maximumThroughput() {
		return new AutoScalingStatisticsPair(maximum(), throughput());
	}
	
	public AutoScalingStatisticsPair getMaximumThroughput() {
		return maximumThroughput();
	}
		
	public AutoScalingStatisticsPair minimumThroughput() {
		return new AutoScalingStatisticsPair(minimum(), throughput());
	}
	
	public AutoScalingStatisticsPair getMinimumThroughput() {
		return minimumThroughput();
	}
	
	public AutoScalingStatisticsPair averageThroughput() {
		return new AutoScalingStatisticsPair(average(), throughput());
	}
	
	public AutoScalingStatisticsPair getAverageThroughput() {
		return averageThroughput();
	}

	public AutoScalingStatisticsPair getAverageOfAverages() {
		return averageOfAverages();
	}
	
	public AutoScalingStatisticsPair averageOfAverages() {
		return new AutoScalingStatisticsPair ( average(), average() );
	}
	
	public AutoScalingStatisticsPair getPercentileOfAverages(double percentile) {
		return new AutoScalingStatisticsPair ( percentile(percentile), average() );
	}
	
	public AutoScalingStatisticsPair getMinimumOfAverages() {
		return minimumOfAverages();
	}
	
	public AutoScalingStatisticsPair minimumOfAverages() {
		return new AutoScalingStatisticsPair ( minimum(), average() );
	}
	
	public AutoScalingStatisticsPair getMaximumOfAverages() {
		return maximumOfAverages();
	}
	
	public AutoScalingStatisticsPair maximumOfAverages() {
		return new AutoScalingStatisticsPair ( maximum(), average() );
	}
	
	public AutoScalingStatisticsPair getMaximumOfMaximums() {
		return maximumOfMaximums();
	}
	
	public AutoScalingStatisticsPair maximumOfMaximums() {
		return new AutoScalingStatisticsPair ( maximum(), maximum() );
	}
	
	public AutoScalingStatisticsPair getMinimumOfMinimums() {
		return minimumOfMinimums();
	}
	
	public AutoScalingStatisticsPair minimumOfMinimums() {
		return new AutoScalingStatisticsPair ( minimum(), minimum() );
	}
}
