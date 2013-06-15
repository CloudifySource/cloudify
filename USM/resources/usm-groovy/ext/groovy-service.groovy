import java.util.concurrent.TimeUnit


service {
	name "groovy"
	type "UNDEFINED"
	
	elastic true
	numInstances 1
	maxAllowedInstances 1
	lifecycle { 
	
		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		preStart {println "This is the preStart event" }
		
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