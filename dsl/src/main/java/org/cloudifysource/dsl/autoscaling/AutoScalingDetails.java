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

import java.io.Serializable;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


/**
 * Domain Object for defining SLA based on statistics and thresholds that triggers 
 * scale out or scale in action.
 * 
 * @author itaif
 * @since 2.1
 * @see Service
 */
@CloudifyDSLEntity(name = "autoScaling", clazz = AutoScalingDetails.class, allowInternalNode = true, allowRootNode = false,
parent = "service")
public class AutoScalingDetails {
	
	private static final long DEFAULT_SAMPLING_PERIOD_SECONDS = 60;

	private static final long DEFAULT_TIME_WINDOW_SECONDS = 5*DEFAULT_SAMPLING_PERIOD_SECONDS;

	private static final AutoScalingStatisticsFactory statisticsFactory = new AutoScalingStatisticsFactory();
	private static final AutoScalingStatistics DEFAULT_TIME_STATISTICS = statisticsFactory.average();
	private static final AutoScalingStatistics DEFAULT_INSTANCES_STATISTICS = statisticsFactory.maximum();
	
	private long samplingPeriodSeconds = DEFAULT_SAMPLING_PERIOD_SECONDS;
	
	private String metric;
	
	private long timeWindowSeconds = DEFAULT_TIME_WINDOW_SECONDS;
	
	private AutoScalingStatistics timeStatistics = DEFAULT_TIME_STATISTICS;
	
	private AutoScalingStatistics instancesStatistics = DEFAULT_INSTANCES_STATISTICS;
	
	private Serializable highThreshold;
	
	private Serializable lowThreshold;
	
	
	public AutoScalingStatistics getTimeStatistics() {
		return timeStatistics;
	}

	public void setTimeStatistics(AutoScalingStatistics timeStatistics) {
		this.timeStatistics = timeStatistics;
	}

	public AutoScalingStatistics getInstancesStatistics() {
		return instancesStatistics;
	}

	public void setInstancesStatistics(AutoScalingStatistics instancesStatistics) {
		this.instancesStatistics = instancesStatistics;
	}

	public Serializable getHighThreshold() {
		return highThreshold;
	}

	public void setHighThreshold(Serializable highThreshold) {
		this.highThreshold = highThreshold;
	}

	public Serializable getLowThreshold() {
		return lowThreshold;
	}

	public void setLowThreshold(Serializable lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public long getTimeWindowSeconds() {
		return timeWindowSeconds;
	}

	public void setTimeWindowSeconds(long timeWindowSeconds) {
		this.timeWindowSeconds = timeWindowSeconds;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public long getSamplingPeriodSeconds() {
		return samplingPeriodSeconds;
	}

	public void setSamplingPeriodSeconds(long samplingPeriodSeconds) {
		this.samplingPeriodSeconds = samplingPeriodSeconds;
	}

	public AutoScalingStatisticsFactory getStatisticsFactory() {
		return statisticsFactory;
	}
}
