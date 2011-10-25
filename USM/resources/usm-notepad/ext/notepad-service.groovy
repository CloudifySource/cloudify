
service {
	name "notepad-service"

	icon "icon.png"
	lifecycle {
		init {

			println "hello"
			println "world"
			println "instance ID: " + context.instanceId
		}
		preStart (["Win.*":"echo starting notepad"])

		//start (["Linux" : "./np.sh"])
		start  "np.bat"
		// postStart "postStart.groovy"

		postStart {
			println "This is the postStart event"
			println "Context: " + context
			println "Context Service: " + context.service
			println "Service Name: " + context.service.name
			println "Admin: " + context.admin
			println "Service Planned Instances: " + context.service.numberOfPlannedInstances
			println "Service Actual Instances: " + context.service.numberOfActualInstances
			if(context.service.numberOfActualInstances > 0) {
				context.service.instances.each {
					println "Service Instance ID: " + it.instanceID
					println "Service Instance Host Address: " + it.hostAddress
					println "Service Instance Host Name: " + it.hostName
				}
			}
		}
	}


}