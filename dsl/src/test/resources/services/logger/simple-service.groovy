

//def dslLogger = java.util.logging.Logger.getLogger("dslLogger")
//def impl = {x ->
//		if (x == null) { 
//			dslLogger.info("null") 
//		} else { 
//			dslLogger.info(x)
//		} 
//}
//Object.metaClass.println = {x->this.println(x)}  
//Object.metaClass.print =  {x->this.print(x)}
//this.metaClass.println = impl
//this.metaClass.print = impl


println "println logger call"
print "print logger call"
println ""

new SomeClass().testPrintln()
new SomeClass().testPrint()


service {
	
	name "simple"
	
	type "WEB_SERVER"
	numInstances 1
	compute {
		//the templeate name is reference to template definition in the cloud driver
		template "BIG_LINUX_32"
	
	}
	lifecycle {
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"

			}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat", "Linux":"run.sh", "Mac.*":"run.sh"])

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}

	
	customCommands ([
		"cmd1" : { println "This is the cmd1 custom command"},
		"cmd3" : { throw new Exception("This is an error test")}
	])

	
	plugins = [
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
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
	]

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
					barLineChart{ metric = "memory"
						axisYUnit Unit.PERCENTAGE
						}
				]
			}
		]
	}
	
}