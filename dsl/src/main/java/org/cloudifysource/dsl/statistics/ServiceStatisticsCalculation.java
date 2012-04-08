package org.cloudifysource.dsl.statistics;

import org.cloudifysource.dsl.statistics.InstancesStatisticsCalculation;
import org.cloudifysource.dsl.statistics.TimeWindowStatisticsCalculation;

/**
 * @see StatisticsCalculationFactory
 * @author itaif
 * @since 2.1
 */
public class ServiceStatisticsCalculation {
	private final InstancesStatisticsCalculation instancesStatistics;
	private final TimeWindowStatisticsCalculation timeStatistics;
	
	public ServiceStatisticsCalculation(InstancesStatisticsCalculation instancesStatistics, TimeWindowStatisticsCalculation timeStatistics) {
		this.instancesStatistics= instancesStatistics;
		this.timeStatistics = timeStatistics;
	}
	
	public TimeWindowStatisticsCalculation getTimeStatistics() {
		return timeStatistics;
	}
	public InstancesStatisticsCalculation getInstancesStatistics() {
		return instancesStatistics;
	}
	
}
