package org.cloudifysource.dsl.autoscaling;

/**
 * @see AutoScalingStatisticsFactory
 * @author itaif
 * @since 2.1
 */
public class AutoScalingStatisticsPair {
	private final AutoScalingStatistics instancesStatistics;
	private final AutoScalingStatistics timeStatistics;
	
	public AutoScalingStatisticsPair(AutoScalingStatistics instancesStatistics, AutoScalingStatistics timeStatistics) {
		this.instancesStatistics= instancesStatistics;
		this.timeStatistics = timeStatistics;
	}
	
	public AutoScalingStatistics getTimeStatistics() {
		return timeStatistics;
	}
	public AutoScalingStatistics getInstancesStatistics() {
		return instancesStatistics;
	}
	
}
