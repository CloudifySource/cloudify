package org.cloudifysource.dsl.statistics;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.statistics.EachSingleInstanceStatistics;

/**
 * Domain POJO for a calculated statistics that is aggregated by time for each instance separately.
 * 
 * @author itaif
 * @since 2.1
 * 
 */
@CloudifyDSLEntity(name = "perInstanceStatistics", clazz = PerInstanceStatisticsDetails.class,
		allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class PerInstanceStatisticsDetails extends AbstractStatisticsDetails {

	public PerInstanceStatisticsDetails() {
		super();
		super.setInstancesStatistics(new EachSingleInstanceStatistics());
	}

	/**
	 * @see AbstractStatisticsDetails#getTimeStatistics()
	 */
	public TimeWindowStatisticsCalculation getStatistics() {
		return getTimeStatistics();
	}

	/**
	 * @see AbstractStatisticsDetails#setTimeStatistics(TimeWindowStatisticsCalculation)
	 */
	public void setStatistics(final TimeWindowStatisticsCalculation timeStatistics) {
		super.setTimeStatistics(timeStatistics);
	}

}
