package org.cloudifysource.dsl.statistics;

/**
 * Base class for serviceStatistics and serviceInstanceStatistics.
 * 
 * @author itaif
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */

public class AbstractStatisticsDetails {

	private static final long DEFAULT_TIME_WINDOW_SECONDS = 5 * 60;

	/**
	 * This helper method is automatically imported to the DSL recipe. import
	 * org.cloudifysource.dsl.autoscaling.AtuoScalingDetails.statisticsFatory as statistics This exposes the {link
	 * {@link StatisticsCalculationFactory} as a singleton. For example: statistics.average, statistics.percentile(30)
	 * 
	 * @see org.cloudifysource.dsl.internal.DSLReader#createCompilerConfiguration() for more details
	 * 
	 */
	private static final StatisticsCalculationFactory STATISTICS_FACTORY = new StatisticsCalculationFactory();

	private static final TimeWindowStatisticsCalculation DEFAULT_TIME_STATISTICS = STATISTICS_FACTORY.average();
	private static final InstancesStatisticsCalculation DEFAULT_INSTANCES_STATISTICS = STATISTICS_FACTORY.average();

	private String name;

	private String metric;

	private long timeRangeSeconds = DEFAULT_TIME_WINDOW_SECONDS;

	private TimeWindowStatisticsCalculation timeStatistics = DEFAULT_TIME_STATISTICS;

	private InstancesStatisticsCalculation instancesStatistics = DEFAULT_INSTANCES_STATISTICS;

	public long getMovingTimeRangeInSeconds() {
		return timeRangeSeconds;
	}

	/**
	 * @param timeRangeSeconds The sliding time window (in seconds) for aggregating per-instance metric samples. The
	 *        number of samples in the time windows equals the time window divided by the sampling period The default
	 *        value is 300 (seconds)
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

	public TimeWindowStatisticsCalculation getTimeStatistics() {
		return timeStatistics;
	}

	/**
	 * @param timeStatistics The algorithm for aggregating metric samples in the specified time window. Metric samples
	 *        are aggregated separately per instance. Default: statistics.average Possible values: statistics.average,
	 *        statistics.minimum, statistics.maximum, statistics.percentile(n)
	 */
	public void setTimeStatistics(final TimeWindowStatisticsCalculation timeStatistics) {
		this.timeStatistics = timeStatistics;
	}

	public InstancesStatisticsCalculation getInstancesStatistics() {
		return instancesStatistics;
	}

	/**
	 * @param instancesStatistics The algorithm use to aggregate timeStatistics from all of the instances. Default
	 *        value: statistics.average. Possible values: statistics.average, statistics.minimum, statistics.maximum,
	 *        statistics.percentile(n)
	 */
	protected void setInstancesStatistics(final InstancesStatisticsCalculation instancesStatistics) {
		this.instancesStatistics = instancesStatistics;
	}

	public String getName() {
		return name;
	}

	/**
	 * @param name - A unique identifier used to reference this statistics.
	 */
	public void setName(final String name) {
		this.name = name;
	}

}
