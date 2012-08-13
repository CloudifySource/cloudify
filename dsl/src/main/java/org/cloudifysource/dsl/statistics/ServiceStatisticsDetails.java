package org.cloudifysource.dsl.statistics;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Domain POJO for a calculated statistics that is aggregated by time and then aggregated accross instances.
 * 
 * @author itaif
 * @since 2.1
 * 
 */
@CloudifyDSLEntity(name = "serviceStatistics", clazz = ServiceStatisticsDetails.class, allowInternalNode = true,
		allowRootNode = false/*
							 * , parent = "service"
							 */)
public class ServiceStatisticsDetails extends AbstractStatisticsDetails {

	/**
	 * @param statistics The algorithm for aggregating metric samples by instances (first argument in the list) and by
	 *        time (the second argument in the list). Metric samples are aggregated separately per instance in the
	 *        specified time range, and then aggregated again for all instances. Default: Statistics.averageOfAverages
	 *        Possible values: Statistics.maximumOfAverages, Statistics.minimumOfAverages, Statistics.averageOfAverages,
	 *        Statistics.percentileOfAverages(90) Statistics.maximumOfMaximums, Statistics.minimumOfMinimums
	 * 
	 *        This has the same effect as setting instancesStatistics and timeStatistics separately. For example:
	 *        statistics Statistics.maximumOfAverages is the same as: timeStatistics Statistics.average
	 *        instancesStatistics Statistics.maximum
	 * 
	 */
	public void setStatistics(final ServiceStatisticsCalculation statistics) {
		super.setInstancesStatistics(statistics.getInstancesStatistics());
		super.setTimeStatistics(statistics.getTimeStatistics());
	}

	public ServiceStatisticsCalculation getStatistics() {
		return new ServiceStatisticsCalculation(getInstancesStatistics(), getTimeStatistics());
	}

	/**
	 * @see AbstractStatisticsDetails#setInstancesStatistics(InstancesStatisticsCalculation)
	 */
	@Override
	public void setInstancesStatistics(final InstancesStatisticsCalculation instancesStatistics) {
		super.setInstancesStatistics(instancesStatistics);
	}
}
