

service {
	
	name "simple"
	icon "icon.png"

	lifecycle {
	//	grrr { println " context is:"}
//		init { println " IN INIT context is:  " + context}
		init {
			println "Var1 is: " + var1
			println "Var2 is: " + var2
		}
		//init (["init.bat", true, "String", 1234])
		
		//preInstall "context.groovy";
//		postInstall {
//			println "This is the postInstall event"
//			println "Context is: " + context
//			println "Instance ID is: " + context.instanceId
//			println "Dir is: " + context.serviceDirectory
//			println ""
//
//			//throw new IllegalStateException("HAHA")
//		}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
		//start (["Linux" : ["run.sh", "param1", "Pa ram 2", "some other \\ param"] ])
		//		start {
		//			def fullPath =  context.dir + "\\run.bat"
		//			println "Executing command: " + fullPath
		//			return  fullPath.execute()
		//			}

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }		
		
		startDetectionTimeoutSecs 10
		startDetection {
			ServiceUtils.isHttpURLAvailable("http://www.google.com") 
		}
	}


	plugins ([
		plugin {
			name "jmx"
			className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"
			config ([

						"Details" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Details"
						],
						"Counter" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Counter"
						],
						"Type" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Type"
						],
						port : 9988
					])
		},
		plugin {
			name "jmx2"
			className "com.gigaspaces.cloudify.usm.jmx.JmxDetails"
			config ([

						"Details" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Details"
						],
						"Counter" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Counter"
						],
						"Type" : [
							"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
							"Type"
						],
						port : 9988
					])
		}
	])

	userInterface {
		metricGroups = [
			metricGroup{
				name = "process"
				metrics = ["cpu", "memory"]
			},
			metricGroup{
				name = "space"
				metrics = ["reads", "writes"]
			}
		]
		widgetGroups = [
			widgetGroup{
				name  ="cpu"
				widgets = [
					balanceGauge{metric = "cpu"},
					barLineChart{metric = "cpu"}
				]
			},
			widgetGroup {
				name = "memory"
				widgets = [
					balanceGauge { metric = "memory" },
					barLineChart{
						metric = "memory"
						axisYUnit Unit.PERCENTAGE
					}
				]
			}
		]
	}


}