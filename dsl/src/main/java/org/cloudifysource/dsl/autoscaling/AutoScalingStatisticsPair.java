package org.cloudifysource.dsl.autoscaling;

/**
 * @see AutoScalingStatisticsFactory
 * @author itaif
 * @since 2.1
 */
public class AutoScalingStatisticsPair {
	private final InstancesStatisticsConfigFactory instancesStatistics;
	private final TimeWindowStatisticsConfigFactory timeStatistics;
	
	public AutoScalingStatisticsPair(InstancesStatisticsConfigFactory instancesStatistics, TimeWindowStatisticsConfigFactory timeStatistics) {
		this.instancesStatistics= instancesStatistics;
		this.timeStatistics = timeStatistics;
	}
	
	public TimeWindowStatisticsConfigFactory getTimeStatistics() {
		return timeStatistics;
	}
	public InstancesStatisticsConfigFactory getInstancesStatistics() {
		return instancesStatistics;
	}
	
}
