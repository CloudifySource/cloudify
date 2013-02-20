package org.cloudifysource.dsl.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.statistics.TimeWindowStatisticsCalculation;
import org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class ThroughputAutoScalingStatistics implements TimeWindowStatisticsCalculation {
	
	/**
	 * Creates a configuration that calculates the change during the specified time window, normalized to change per second.
	 * This is done by dividing the change in metric value by time normalized to seconds.
	 * Use when the metric exposed by the service the is total number of requests.
	 * For example, given the following total number of requests metric in a 20 seconds time window, 
	 * the throughput would be (1600-1000)/((120000-100000)/1000)=(1600-1000)/(120-100)=600/20=30 requests/seconds
	 * 	{ timestamp: 100000ms , value: 1000 } , { timestamp: 110000ms, value: 1300}, { timestamp: 120000ms, value: 1600}
	 */
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		return 
			new ThroughputTimeWindowStatisticsConfigurer()
			.timeWindow(timeWindow, timeUnit)
			.create();
	}
}
