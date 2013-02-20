package org.cloudifysource.dsl.statistics;


/**
 * @see StatisticsCalculationFactory
 * @author itaif
 * @since 2.1
 */
public class ServiceStatisticsCalculation {

	private final InstancesStatisticsCalculation instancesStatistics;
	private final TimeWindowStatisticsCalculation timeStatistics;

	public ServiceStatisticsCalculation(final InstancesStatisticsCalculation instancesStatistics,
			final TimeWindowStatisticsCalculation timeStatistics) {
		this.instancesStatistics = instancesStatistics;
		this.timeStatistics = timeStatistics;
	}

	public TimeWindowStatisticsCalculation getTimeStatistics() {
		return timeStatistics;
	}

	public InstancesStatisticsCalculation getInstancesStatistics() {
		return instancesStatistics;
	}

}
