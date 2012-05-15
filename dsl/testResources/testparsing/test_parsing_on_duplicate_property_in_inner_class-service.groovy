import java.util.concurrent.atomic.AtomicLong;

// This service is a mock for recipe parsing unit test
service {
  
	name "scalingRules"
 
  lifecycle {

      init { println "This is the init event" }
	  init { println "This is the init event" }
      start "run.groovy"
	}

}