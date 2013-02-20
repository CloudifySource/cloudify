import java.util.concurrent.atomic.AtomicLong;

// This service is a mock for recipe parsing unit test
service {
  
	name "scalingRules"
	type "WEB_SERVER"
  lifecycle {

      init { println "This is the init event" }
	  init { println "This is the init event" }
      start "run.groovy"
	}

}