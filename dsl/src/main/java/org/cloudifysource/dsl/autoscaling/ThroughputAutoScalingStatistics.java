package org.cloudifysource.dsl.autoscaling;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class ThroughputAutoScalingStatistics implements TimeWindowStatisticsConfigFactory {
	
	@Override
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		return 
			new ThroughputTimeWindowStatisticsConfigurer()
			.timeWindow(timeWindow, timeUnit)
			.create();
	}
}
