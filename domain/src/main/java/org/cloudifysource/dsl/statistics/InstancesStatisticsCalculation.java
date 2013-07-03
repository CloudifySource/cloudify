package org.cloudifysource.dsl.statistics;

	
public interface InstancesStatisticsCalculation {
	/**
	 * Creates a configuration that reduces values of a metric from several service instances, into a single value.
	 */
	public InstancesStatisticsConfig createInstancesStatistics();
}
