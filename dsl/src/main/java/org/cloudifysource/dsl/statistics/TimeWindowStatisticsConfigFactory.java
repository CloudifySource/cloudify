package org.cloudifysource.dsl.statistics;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public interface TimeWindowStatisticsConfigFactory {

	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit);

}
