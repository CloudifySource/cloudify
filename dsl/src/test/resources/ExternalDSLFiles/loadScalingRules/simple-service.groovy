import java.util.concurrent.TimeUnit;

service {
	name "tomcat"
	type "WEB_SERVER"
	

	lifecycle {
		start "tomcat_start.groovy"
	}

	
	scaleCooldownInSeconds 20
	samplingPeriodInSeconds 1

	// Defines an automatic scaling rule based on "counter" metric value
	scalingRules load("scalingRules.groovy")

}