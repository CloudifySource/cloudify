package org.cloudifysource.dsl.statistics;


/**
 * Base class for serviceStatistics and serviceInstanceStatistics
 * 
 * @author itaif
 * @since 2.1
 * @see org.cloudifysource.dsl.Service
 */

public class AbstractStatisticsDetails {


	private static final long DEFAULT_TIME_WINDOW_SECONDS = 5 * 60;
	
	/**
	 * This helper method is automatically imported to the DSL recipe.
	 * import org.cloudifysource.dsl.autoscaling.AtuoScalingDetails.statisticsFatory as statistics
	 * This exposes the {link {@link AutoScalingStatisticsFactory} as a singleton. 
	 * For example: statistics.average, statistics.percentile(30)
	 * @see org.cloudifysource.dsl.internal.DSLReader#createCompilerConfiguration() for more details
	 *
	 */
	private static final AutoScalingStatisticsFactory STATISTICS_FACTORY = new AutoScalingStatisticsFactory();
	
	private static final AverageAutoScalingStatistics DEFAULT_TIME_STATISTICS = STATISTICS_FACTORY.average();
	private static final AverageAutoScalingStatistics DEFAULT_INSTANCES_STATISTICS = STATISTICS_FACTORY.average();

	private String name;
	
	private String metric;

	private long timeRangeSeconds = DEFAULT_TIME_WINDOW_SECONDS;
	
	private TimeWindowStatisticsConfigFactory timeStatistics = DEFAULT_TIME_STATISTICS;
	
	private InstancesStatisticsConfigFactory instancesStatistics = DEFAULT_INSTANCES_STATISTICS;

	public long getMovingTimeRangeInSeconds() {
		return timeRangeSeconds;
	}

	/**
	 * @param timeRangeSeconds The sliding time window (in seconds) for aggregating per-instance metric samples.
     * The number of samples in the time windows equals the time window divided by the sampling period
     * The default value is 300 (seconds)
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
	
	public TimeWindowStatisticsConfigFactory getTimeStatistics() {
		return timeStatistics;
	}

	/**
	 * @param timeStatistics 
	 * 			The algorithm for aggregating metric samples in the specified time window.
     * 			Metric samples are aggregated separately per instance.
     * 			Default: statistics.average
     * 			Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
     */
	protected void setTimeStatistics(final TimeWindowStatisticsConfigFactory timeStatistics) {
		this.timeStatistics = timeStatistics;
	}

	public InstancesStatisticsConfigFactory getInstancesStatistics() {
		return instancesStatistics;
	}

	/**
	 * @param instancesStatistics
	 *            The algorithm use to aggregate timeStatistics from all of the
	 *            instances. 
	 *            Default value: statistics.average. 
	 *            Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
	 */
	protected void setInstancesStatistics(final InstancesStatisticsConfigFactory instancesStatistics) {
		this.instancesStatistics = instancesStatistics;
	}

	public String getName() {
		return name;
	}

	/**
	 * @param name - A unique identifier used to reference this statistics.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
