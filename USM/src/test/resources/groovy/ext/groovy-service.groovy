import java.util.concurrent.TimeUnit




service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	lifecycle {

		preStart {
			new File(context.serviceDirectory + "/marker.txt").delete()
		}
		start "run.groovy"


		startDetection {
			def file = new File(context.serviceDirectory + "/marker.txt")
			println "Checking if file exists: " + file
			new File(context.serviceDirectory + "/marker.txt").exists()
		}
	}
}