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

	// average
	public AutoScalingStatistics getAverage() {
		return average();
	}
	
	// average()
	public AutoScalingStatistics average() {
		return new AverageAutoScalingStatistics();
	}

	// maximum
	public AutoScalingStatistics getMaximum() {
		return maximum();
	}
	
	// maximum()
	public AutoScalingStatistics maximum() {
		return new MaximumAutoScalingStatistics();
	}

	// minimum
	public AutoScalingStatistics getMinimum() {
		return minimum();
	}
	
	// minimum()
	public AutoScalingStatistics minimum() {
		return new MinimumAutoScalingStatistics();
	}

	// percentile(30)
	public AutoScalingStatistics percentile(double percentile) {
		return new PercentileAutoScalingStatistics(percentile);
	}
	
	// median()
	public AutoScalingStatistics median() {
		return new PercentileAutoScalingStatistics(50.0);
	}
	
	// median
	public AutoScalingStatistics getMedian() {
		return median();
	}
	

}
