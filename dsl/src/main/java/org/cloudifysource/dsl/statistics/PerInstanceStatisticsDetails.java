package org.cloudifysource.dsl.statistics;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


/**
 * Domain POJO for a calculated statistics that is aggregated by time for each instance separately.
 * 
 * @author itaif
 * @since 2.1
 * 
 */
@CloudifyDSLEntity(name = "perInstanceStatistics", clazz = PerInstanceStatisticsDetails.class, allowInternalNode = true, allowRootNode = false,
parent = "service")
public class PerInstanceStatisticsDetails extends AbstractStatisticsDetails {

	public PerInstanceStatisticsDetails() {
		super();
		super.setInstancesStatistics(new EachSingleInstanceStatistics());
	}

	/**
	 * @see #getTimeStatistics()
	 */
	public TimeWindowStatisticsConfigFactory getStatistics() {
		return getTimeStatistics();
	}

	/**
	 * @see #setTimeStatistics(TimeWindowStatisticsConfigFactory)
	 */
	public void setStatistics(final TimeWindowStatisticsConfigFactory timeStatistics) {
		setTimeStatistics(timeStatistics);
	}

	@Override
	public TimeWindowStatisticsConfigFactory getTimeStatistics() {
		return super.getTimeStatistics();
	}

	@Override
	public void setTimeStatistics(final TimeWindowStatisticsConfigFactory timeStatistics) {
		super.setTimeStatistics(timeStatistics);
	}

	@Override
	public InstancesStatisticsConfigFactory getInstancesStatistics() {
		return super.getInstancesStatistics();
	}
}
