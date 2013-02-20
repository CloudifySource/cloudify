package org.cloudifysource.dsl.statistics;

import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;

public interface InstancesStatisticsCalculation {
	
	/**
	 * Creates a configuration that reduces values of a metric from several service instances, into a single value.
	 */
	public InstancesStatisticsConfig createInstancesStatistics();
}
