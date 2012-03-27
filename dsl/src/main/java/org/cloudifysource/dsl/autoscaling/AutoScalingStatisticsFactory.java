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
	public AutoScalingStatistics getAverage() {
		return average();
	}
	
	/**
	 * supports statistics.average() in the recipe (with brackets)
	 */
	public AutoScalingStatistics average() {
		return new AverageAutoScalingStatistics();
	}

	/**
	 * supports statistics.maximum in the recipe (without brackets)
	 */
	public AutoScalingStatistics getMaximum() {
		return maximum();
	}
	
	/**
	 * supports statistics.maximum() in the recipe (with brackets)
	 */
	public AutoScalingStatistics maximum() {
		return new MaximumAutoScalingStatistics();
	}

	/**
	 * supports statistics.minimum() in the recipe (without brackets)
	 */
	public AutoScalingStatistics getMinimum() {
		return minimum();
	}
	
	/**
	 * supports statistics.minimum() in the recipe (with brackets)
	 */
	public AutoScalingStatistics minimum() {
		return new MinimumAutoScalingStatistics();
	}

	/**
	 * supports statistics.percentile(30) in the recipe (with brackets)
	 */
	public AutoScalingStatistics percentile(double percentile) {
		return new PercentileAutoScalingStatistics(percentile);
	}
	
	/**
	 * supports statistics.median() in the recipe (with brackets)
	 */
	public AutoScalingStatistics median() {
		return new PercentileAutoScalingStatistics(50.0);
	}
	
	/**
	 * supports statistics.median in the recipe (without brackets)
	 */
	public AutoScalingStatistics getMedian() {
		return median();
	}
}
