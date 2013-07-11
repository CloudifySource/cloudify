package org.cloudifysource.domain.internal.statistics;

import org.cloudifysource.domain.statistics.EachSingleInstanceStatisticsConfig;
import org.cloudifysource.domain.statistics.InstancesStatisticsCalculation;
import org.cloudifysource.domain.statistics.InstancesStatisticsConfig;

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
