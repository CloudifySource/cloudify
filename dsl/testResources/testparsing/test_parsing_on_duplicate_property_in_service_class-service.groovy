// This service is a mock for recipe parsing unit test
service {
	icon "icon.jpg"
	icon "icon.jpg"
	name "scalingRules"
 
  lifecycle {

	  init { println "This is the init event" }
      start "run.groovy"
	}

}