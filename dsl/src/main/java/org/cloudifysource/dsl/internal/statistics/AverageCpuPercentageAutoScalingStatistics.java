package org.cloudifysource.dsl.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.statistics.TimeWindowStatisticsCalculation;
import org.openspaces.admin.pu.statistics.CpuPercentageTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class AverageCpuPercentageAutoScalingStatistics implements
		TimeWindowStatisticsCalculation {

	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(
			long timeWindow, TimeUnit timeUnit) {
		return 
				new CpuPercentageTimeWindowStatisticsConfigurer()
				.timeWindow(timeWindow, timeUnit)
				.create();
	}

}
