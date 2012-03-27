import java.util.concurrent.atomic.AtomicLong;

// This service is a mock for recipe parsing unit test
service {
  
	name "autoscaling"
 
  lifecycle {

      //sleep forever
      start "run.groovy"
	}

	// global flag that enables changing number of instances for this service
	elastic true

  // the initial number of instances
  numInstances 2
     
  // The minimum number of service instances
  minNumInstances 2
    
  // The maximum number of service instances
  maxNumInstances 20
     
	// Defines an automatic scaling rule based on "counter" metric value
  autoScaling {
   
    //The time (in seconds) between two consecutive metric samples
    samplingPeriodSeconds 1
         
    // The name of the metric that is the basis for the scale rule decision
    metric "counter"
    
    // The sliding time window (in secods) for aggregating per-instance metric samples
    // The number of samples in the time windows equals the time window divided by the sampling period
    timeWindowSeconds 5
    
    // (Optional)
    // The algorithm for aggregating metric samples in the specified time window.
    // Metric samples are aggregated separately per instance.
    // Default: statistics.average
    // Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
    timeStatistics statistics.average
    
    // (Optional)
    // The aggregation of all instances' timeStatistics
    // Default value: statistics.maximum
    // Possible values: statistics.average, statistics.minimum, statistics.maximum, statistics.percentile(n)
    instancesStatistics statistics.maximum
    
    // The instancesStatistics over which the number of instances is increased or decreased
    highThreshold 90
    
    // The instancesStatistics below which the number of instances is increased or decreased
    lowThreshold 10

  }
}