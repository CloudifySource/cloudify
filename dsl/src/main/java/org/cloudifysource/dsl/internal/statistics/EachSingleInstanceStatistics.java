package org.cloudifysource.dsl.internal.statistics;

import org.cloudifysource.dsl.statistics.InstancesStatisticsCalculation;
import org.openspaces.admin.pu.statistics.EachSingleInstanceStatisticsConfig;
import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;

/**
 * 
 * @author itaif
 * @since 2.1
 */

public class EachSingleInstanceStatistics implements InstancesStatisticsCalculation {

	/**
	 * Creates a configuration that returns each instance statistics as it is
	 */
	@Override
	public InstancesStatisticsConfig createInstancesStatistics() {
		return new EachSingleInstanceStatisticsConfig();
	}

}
