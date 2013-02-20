scalingRule {

	serviceStatistics {
		metric "Total Process Cpu Time"
		timeStatistics Statistics.averageCpuPercentage
		instancesStatistics Statistics.maximum
		movingTimeRangeInSeconds 20
	}

	highThreshold {
		value 40
		instancesIncrease 1
	}

	lowThreshold {
		value 25
		instancesDecrease 1
	}
}
