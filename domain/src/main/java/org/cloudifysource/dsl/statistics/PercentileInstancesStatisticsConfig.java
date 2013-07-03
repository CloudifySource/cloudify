package org.cloudifysource.dsl.statistics;

public class PercentileInstancesStatisticsConfig extends InstancesStatisticsConfig {

	private double percentile;

	public double getPercentile() {
		return percentile;
	}

	public void setPercentile(final double percentile) {
		this.percentile = percentile;
	}
}
