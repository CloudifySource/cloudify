package org.cloudifysource.dsl.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.statistics.TimeWindowStatisticsCalculation;
import org.openspaces.admin.pu.statistics.CpuPercentageTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class AverageCpuPercentageAutoScalingStatistics implements
		TimeWindowStatisticsCalculation {

	/**
	 * Creates a configuration that calculates the change during the specified time window, normalized to change per
	 * milliseconds multiplied by 100. This is done by dividing the change in metric value by time normalized to
	 * milliseconds and then multiplying by 100. Use when the metric exposed by the service the is total cpu usage of a
	 * process or a machine. For example, given the following total cpu metric in a 20 seconds time window, the average
	 * cpu would be 100*(1600-1000)/(120000-100000)=100*(600/20000)=100*0.03 CPU ticks per millisecond = 3% CPU {
	 * timestamp: 100000ms , value: 1000 } , { timestamp: 110000ms, value: 1300}, { timestamp: 120000ms, value: 1600}
	 */
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(
			final long timeWindow, final TimeUnit timeUnit) {
		return new CpuPercentageTimeWindowStatisticsConfigurer()
				.timeWindow(timeWindow, timeUnit)
				.create();
	}

}
