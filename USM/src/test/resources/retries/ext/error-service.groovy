import java.util.concurrent.TimeUnit


service {
	name "groovyError"
	type "UNDEFINED"
	
	elastic true
	numInstances 1
	maxAllowedInstances 1
	retries retriesLimit
	
	lifecycle { 
	
		init { println "This is the init event of the groovy error service" }
		preInstall {println "This is the preInstall event of the groovy error service" }
		install {
			println "This is the install event of the groovy error service. About to fail the install event"
			throw new Exception("Groovy error service install event - this is an error test")
		}
				
		start "run.groovy" 
		
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
		
		startDetection {
			new File(context.serviceDirectory + "/marker.txt").exists()
		}
		
		monitors {
			def time = System.currentTimeMillis()

			return [
				"time" : time]
		}
		
		
	}
}