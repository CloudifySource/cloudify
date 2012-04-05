package org.cloudifysource.dsl.autoscaling;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.statistics.InstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfigurer;
import org.openspaces.admin.pu.statistics.TimeWindowStatisticsConfig;

public class ThroughputAutoScalingStatistics extends AutoScalingStatistics {
	
	@Override
	public TimeWindowStatisticsConfig toTimeWindowStatistics(long timeWindow, TimeUnit timeUnit) {
		return 
			new ThroughputTimeWindowStatisticsConfigurer()
			.timeWindow(timeWindow, timeUnit)
			.create();
	}

	@Override
	public InstancesStatisticsConfig toInstancesStatistics() {
		throw 
		    new UnsupportedOperationException("toInstancesStatistics is not supported for type ThroughputAutoScalingStatistics");
	}


}
