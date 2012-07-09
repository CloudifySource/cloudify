import java.util.concurrent.atomic.AtomicLong;

// This service is a mock for recipe parsing unit test
service {
  
	name "scalingRules"
	type "WEB_SERVER"
  lifecycle {

      //sleep forever
      start "run.groovy"
	}

	// global flag that enables changing number of instances for this service
	elastic true

  // the initial number of instances
  numInstances 2
     
  // The minimum number of service instances
  minAllowedInstances 2
    
  // The maximum number of service instances
  maxAllowedInstances 20

  scaleCooldownInSeconds 1
  scaleInCooldownInSeconds 1
  scaleOutCooldownInSeconds 1

  //The time (in seconds) between two consecutive metric samples
  samplingPeriodInSeconds 1
  
  serviceStatistics ([
    serviceStatistics {
      
      name "avgOfAvg.counter"
      
      // The name of the metric that is the basis for the scale rule decision
      metric "counter"
      
      // The moving time range (in seconds) for aggregating per-instance metric samples
      // The number of samples in the time windows equals the time window divided by the sampling period plus one.
      movingTimeRangeInSeconds 5
      
      // (Optional)
      // The algorithm for aggregating metric samples in the specified time window.
      // Metric samples are aggregated separately per instance.
      // Default: Statistics.average
      // Possible values: Statistics.average, Statistics.minimum, Statistics.maximum, Statistics.percentile(n)
      timeStatistics Statistics.average
      
      // (Optional)
      // The aggregation of all instances' timeStatistics
      // Default value: Statistics.maximum
      // Possible values: Statistics.average, Statistics.minimum, Statistics.maximum, Statistics.percentile(n)
      instancesStatistics Statistics.average
      
    }
    ])
    
    perInstanceStatistics([
    perInstanceStatistics{
      
      name "avg.counter"
      
      // The name of the metric that is the basis for the scale rule decision
      metric "counter"
   
      statistics Statistics.average
      
      // The moving time range (in seconds) for aggregating per-instance metric samples
      // The number of samples in the time windows equals the time window divided by the sampling period plus one.
      movingTimeRangeInSeconds 5
      
      // (Optional)
      // The algorithm for aggregating metric samples in the specified time window.
      // Metric samples are aggregated separately per instance.
      // Default: Statistics.average
      // Possible values: Statistics.average, Statistics.minimum, Statistics.maximum, Statistics.percentile(n)
      timeStatistics Statistics.average
    }
  ])
  
	// Defines an automatic scaling rule based on "counter" metric value
  scalingRules ([
    scalingRule {
      
      serviceStatistics "avgOfAvg.counter"
      
      highThreshold {
          
          // The value above which the number of instances is increased
          value 90
          
          // The number of instances to increase when above threshold
          instancesIncrease 1
        }
        
      lowThreshold {
          // The value below which the number of instances is decreased
          value 10
          
          // The number of instances to decrease when below threshold
          instancesDecrease 1
       }
    },
    scalingRule {
      
      serviceStatistics {
        
        metric "counter"
     
        statistics Statistics.averageOfAverages
        
        movingTimeRangeInSeconds 5
        
      }
      
      highThreshold {
          
          // The value above which the number of instances is increased
          value 90
          
          // The number of instances to increase when above threshold
          instancesIncrease 1
        }
        
      lowThreshold {
          // The value below which the number of instances is decreased
          value 10
          
          // The number of instances to decrease when below threshold
          instancesDecrease 1
       }
    }
 ])
}