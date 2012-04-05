package org.cloudifysource.dsl.autoscaling;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public interface TimeWindowStatisticsConfigFactory {

	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit);

}
