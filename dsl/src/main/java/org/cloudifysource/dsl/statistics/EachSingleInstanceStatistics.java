package org.cloudifysource.dsl.statistics;

import org.openspaces.admin.pu.statistics.EachSingleInstanceStatisticsConfig;
import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;

/**
 * 
 * @author itaif
 * @since 2.1
 */

public class EachSingleInstanceStatistics implements InstancesStatisticsConfigFactory {

	@Override
	public InstancesStatisticsConfig createInstancesStatistics() {
		return new EachSingleInstanceStatisticsConfig();
	}

}
