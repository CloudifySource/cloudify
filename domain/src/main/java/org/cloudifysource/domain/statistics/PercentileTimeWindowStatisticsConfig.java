package org.cloudifysource.domain.statistics;

/**
 * 
 * @author adaml
 *
 */
public class PercentileTimeWindowStatisticsConfig extends TimeWindowStatisticsConfig {

	private double percentile;

	public double getPercentile() {
		return percentile;
	}

	public void setPercentile(final double percentile) {
		this.percentile = percentile;
	}
}
