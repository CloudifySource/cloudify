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

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


/**
 * Domain Object for defining SLA based on statistics and thresholds that triggers 
 * scale out or scale in action.
 * 
 * @author itaif
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */
@CloudifyDSLEntity(name = "scalingRules", clazz = ScalingRulesDetails.class, 
	allowInternalNode = true, allowRootNode = false, parent = "service")
public class ScalingRulesDetails {
	
	private static final long DEFAULT_SAMPLING_PERIOD_SECONDS = 60;

	private static final long DEFAULT_TIME_WINDOW_SECONDS = 5 * DEFAULT_SAMPLING_PERIOD_SECONDS;

	/**
	 * This helper method is automatically imported to the DSL recipe.
	 * import org.cloudifysource.dsl.autoscaling.AtuoScalingDetails.statisticsFatory as statistics
	 * This exposes the {link {@link AutoScalingStatisticsFactory} as a singleton. 
	 * For example: statistics.average, statistics.percentile(30)
	 * @see org.cloudifysource.dsl.internal.DSLReader#createCompilerConfiguration() for more details
	 *
	 */
	private static final AutoScalingStatisticsFactory STATISTICS_FACTORY = new AutoScalingStatisticsFactory();
	
	private static final AverageAutoScalingStatistics DEFAULT_TIME_STATISTICS = STATISTICS_FACTORY.average();
	private static final AverageAutoScalingStatistics DEFAULT_INSTANCES_STATISTICS = STATISTICS_FACTORY.average();
	
	private long samplingPeriodInSeconds = DEFAULT_SAMPLING_PERIOD_SECONDS;
	
	private String metric;
	
	private long timeRangeSeconds = DEFAULT_TIME_WINDOW_SECONDS;
	
	private TimeWindowStatisticsConfigFactory timeStatistics = DEFAULT_TIME_STATISTICS;
	
	private InstancesStatisticsConfigFactory instancesStatistics = DEFAULT_INSTANCES_STATISTICS;
	
	private HighThreshold highThreshold;
	
	private LowThreshold lowThreshold;

	/**
	 * @param statistics 
	 * 			    The algorithm for aggregating metric samples by instances (first argument in the list) and by time (the second argument in the list).
     *              Metric samples are aggregated separately per instance in the specified time range,
     *	            and then aggregated again for all instances.
     *              Default: Statistics.averageOfAverages
     * 		        Possible values: Statistics.maximumOfAverages, Statistics.minimumOfAverages, Statistics.averageOfAverages, Statistics.percentileOfAverages(90)
     *                               Statistics.maximumOfMaximums, Statistics.minimumOfMinimums
     *                               
     * This has the same effect as setting instancesStatistics and timeStatistics separately. 
     * For example: 
     * statistics Statistics.maximumOfAverages
     * is the same as:
     * timeStatistics Statistics.average
     * instancesStatistics Statistics.maximum
     *
     */
	public void setStatistics(final AutoScalingStatisticsPair statistics) {
		setInstancesStatistics(statistics.getInstancesStatistics());
		setTimeStatistics(statistics.getTimeStatistics());
	}
	
	public AutoScalingStatisticsPair getStatistics() {
		return new AutoScalingStatisticsPair (getInstancesStatistics(), getTimeStatistics());
	}

	public TimeWindowStatisticsConfigFactory getTimeStatistics() {
		return timeStatistics;
	}

	/**
	 * @param timeStatistics 
	 * 			The algorithm for aggregating metric samples in the specified time window.
     * 			Metric samples are aggregated separately per instance.
     * 			Default: statistics.average
     * 			Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
     */
	public void setTimeStatistics(final TimeWindowStatisticsConfigFactory timeStatistics) {
		this.timeStatistics = timeStatistics;
	}

	public InstancesStatisticsConfigFactory getInstancesStatistics() {
		return instancesStatistics;
	}

	/**
	 * @param instancesStatistics
	 *            The algorithm use to aggregate timeStatistics from all of the
	 *            instances. 
	 *            Default value: statistics.average. 
	 *            Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
	 */
	public void setInstancesStatistics(final InstancesStatisticsConfigFactory instancesStatistics) {
		this.instancesStatistics = instancesStatistics;
	}

	public HighThreshold getHighThreshold() {
		return highThreshold;
	}

	/**
	 * @param highThreshold
	 * 			a wrapper object for the breach value and the increase action value
	 */
	public void setHighThreshold(final HighThreshold highThreshold) {
		this.highThreshold = highThreshold;
	}

	public LowThreshold getLowThreshold() {
		return lowThreshold;
	}

	/**
	 * @param highThreshold
	 * 			a wrapper object for the breach value and the decrease action value
	 */
	public void setLowThreshold(final LowThreshold lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public long getMovingTimeRangeInSeconds() {
		return timeRangeSeconds;
	}

	/**
	 * @param timeRangeSeconds The sliding time window (in seconds) for aggregating per-instance metric samples.
     * The number of samples in the time windows equals the time window divided by the sampling period
     * The default value is 300 (seconds)
	 */
	public void setMovingTimeRangeInSeconds(final long timeRangeSeconds) {
		this.timeRangeSeconds = timeRangeSeconds;
	}

	public String getMetric() {
		return metric;
	}

	/**
	 * @param metric The name of the metric that is the basis for the scale rule decision.
	 */
	public void setMetric(final String metric) {
		this.metric = metric;
	}

	public long getSamplingPeriodInSeconds() {
		return samplingPeriodInSeconds;
	}

	/**
	 * @param samplingPeriodInSeconds The time (in seconds) between two consecutive metric samples.
	 */
	public void setSamplingPeriodInSeconds(final long samplingPeriodInSeconds) {
		this.samplingPeriodInSeconds = samplingPeriodInSeconds;
	}
}
