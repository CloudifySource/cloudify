
service {
	name "kitchensink-service"
	icon "icon.png"

	lifecycle{
		// DO NOT CHANGE THE PRINTOUTS - SGTEST LOOKS FOR THEM!
		init { println "init fired " + context }
		preInstall{ println "preInstall fired ${var2}"}
		postInstall{ println "postInstall fired " + var1 }
		preStart{ println "preStart fired " + var2 }
		start ([ "Linux": "run.sh -dieOnParentDeath true -port 7777" ,
					"Win.*": "run.bat -dieOnParentDeath true -port 7777" ])

		//postStart "post_start.groovy"

		preStop ([
			"pre_stop.groovy",
			"true",
			"String_with_Spaces",
			"1234"
		])
		postStop{ println "postStop fired" }
		shutdown {
			println "shutdown fired"
			sleep 15000 // sleep so that the test can pick up the event printouts from the log
		}

		details(["stam":{"HA HA HAAAAAAAAAAAAAAAAAAA"},
			"SomeKey":{"22222222222222222222222222"}])
		monitors (["NumberTwo":{println context
			return 2},
			"NumberOne":{println context
				return "1"}])

		startDetection {
			println context
			 ServiceUtils.isPortsOccupied([7777])
		}
	}


	customCommands ([
				"cmd1" : { println "This is the cmd1 custom command"},
				"cmd2" : { throw new Exception("This is the cmd2 custom command - This is an error test")},
				"cmd3" : { "This is the cmd3 custom command. Service Dir is: " + context.serviceDirectory },
				"cmd4" : "context_command.groovy",
				"cmd5" : {"this is the custom parameters command. expecting 123: "+1+x+y}
			])


	plugins ([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [7777],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
		plugin{

			name "JMX Metrics"

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
						port : 9999
					])
		},
		plugin{

			name "JMX Details"

			className "org.cloudifysource.usm.jmx.JmxDetails"

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
						port : 9999
					])
		}
	])

	customProperties ([
				"TailerInterval": "1"
			])

}