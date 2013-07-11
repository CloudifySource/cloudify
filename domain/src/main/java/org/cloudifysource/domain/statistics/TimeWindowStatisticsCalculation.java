package org.cloudifysource.domain.statistics;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author adaml
 *
 */
public interface TimeWindowStatisticsCalculation {

	/**
	 * Creates a configuration that reduces values of a metric time series, into a single value.
	 */
	public TimeWindowStatisticsConfig createTimeWindowStatistics(long timeWindow, TimeUnit timeUnit);

}
