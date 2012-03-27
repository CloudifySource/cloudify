package org.cloudifysource.dsl.autoscaling;

public class AutoScalingStatisticsFactory {

	// average
	public AutoScalingStatistics getAverage() {
		return average();
	}
	
	// average()
	public AutoScalingStatistics average() {
		return new AverageAutoScalingStatistics();
	}

	// maximum
	public AutoScalingStatistics getMaximum() {
		return maximum();
	}
	
	// maximum()
	public AutoScalingStatistics maximum() {
		return new MaximumAutoScalingStatistics();
	}

	// minimum
	public AutoScalingStatistics getMinimum() {
		return minimum();
	}
	
	// minimum()
	public AutoScalingStatistics minimum() {
		return new MinimumAutoScalingStatistics();
	}

	// percentile(30)
	public AutoScalingStatistics percentile(double percentile) {
		return new PercentileAutoScalingStatistics(percentile);
	}
	
	// median()
	public AutoScalingStatistics median() {
		return new PercentileAutoScalingStatistics(50.0);
	}
	
	// median
	public AutoScalingStatistics getMedian() {
		return median();
	}
	

}
