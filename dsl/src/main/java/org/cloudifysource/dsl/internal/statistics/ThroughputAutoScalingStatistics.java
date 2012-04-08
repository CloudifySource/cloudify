package org.cloudifysource.dsl.internal.statistics;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.statistics.TimeWindowStatisticsCalculation;
import org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class ThroughputAutoScalingStatistics implements TimeWindowStatisticsCalculation {
	
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		return 
			new ThroughputTimeWindowStatisticsConfigurer()
			.timeWindow(timeWindow, timeUnit)
			.create();
	}
}
