import java.util.concurrent.TimeUnit;

service {
	name "tomcat"
	icon "tomcat.gif"
	type "WEB_SERVER"
	
    elastic true
	numInstances 1
	minAllowedInstances 1
	maxAllowedInstances 2
	
	def portIncrement =  context.isLocalCloud() ? context.getInstanceId()-1 : 0		
	
	def currJmxPort = jmxPort + portIncrement
	def currHttpPort = port + portIncrement
	def currAjpPort = ajpPort + portIncrement
	
	compute {
		template "SMALL_LINUX"
	}

	lifecycle {
		install "tomcat_install.groovy"
		start "tomcat_start.groovy"
		postStart "tomcat_post_start.groovy"
		preStop "tomcat_stop.groovy"
		startDetectionTimeoutSecs 240
		startDetection {
			println "tomcat-service.groovy(startDetection): arePortsFree http=${currHttpPort} ajp=${currAjpPort} ..."
			!ServiceUtils.arePortsFree([currHttpPort, currAjpPort] )
		}
	}

	customCommands ([       	
		"updateWar" : {warUrl -> 
			println "tomcat-service.groovy(updateWar custom command): warUrl is ${warUrl}..."
			context.attributes.thisService["warUrl"] = "${warUrl}"
			println "tomcat-service.groovy(updateWar customCommand): invoking updateWarFile custom command ..."
			tomcatService = context.waitForService(serviceName, 60, TimeUnit.SECONDS)
			tomcatInstances=tomcatService.waitForInstances(tomcatService.numberOfPlannedInstances,60, TimeUnit.SECONDS)				
			instanceProcessID=context.getInstanceId()			                       
			tomcatInstances.each {
				if ( instanceProcessID == it.instanceID ) {
					println "tomcat-service.groovy(updateWar customCommand):  instanceProcessID is ${instanceProcessID} now invoking updateWarFile..."
					it.invoke("updateWarFile")
				}
			}
						
			println "tomcat-service.groovy(updateWar customCommand): End"
			return true
		} ,
		 
		"updateWarFile" : "updateWarFile.groovy"
    ])

	plugins([
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config([
						"Current Http Threads Busy": [
							"Catalina:type=ThreadPool,name=\"http-bio-${currHttpPort}\"",
							"currentThreadsBusy"
						],
						"Current Http Threads Count": [
							"Catalina:type=ThreadPool,name=\"http-bio-${currHttpPort}\"",
							"currentThreadCount"
						],
						"Backlog": [
							"Catalina:type=ProtocolHandler,port=${currHttpPort}",
							"backlog"
						],
						"Active Sessions":[
							"Catalina:type=Manager,context=/${appFolder},host=localhost",
							"activeSessions"
						],
						"Total Requests Count": [
							"Catalina:type=GlobalRequestProcessor,name=\"http-bio-${currHttpPort}\"",
							"requestCount"
						],
						port: "${currJmxPort}"

					])
		}
	])

	userInterface {

		metricGroups = ([
			metricGroup {

				name "process"

				metrics([
					"Process Cpu Usage",
					"Total Process Virtual Memory",
					"Num Of Active Threads"
				])
			} ,
			metricGroup {

				name "http"

				metrics([
					"Current Http Threads Busy",
					"Current Http Threads Count",
					"Backlog",
					"Total Requests Count"
				])
			} ,

		]
		)

		widgetGroups = ([
			widgetGroup {
				name "Process Cpu Usage"
				widgets ([
					balanceGauge{metric = "Process Cpu Usage"},
					barLineChart{
						metric "Process Cpu Usage"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
			widgetGroup {
				name "Total Process Virtual Memory"
				widgets([
					balanceGauge{metric = "Total Process Virtual Memory"},
					barLineChart {
						metric "Total Process Virtual Memory"
						axisYUnit Unit.MEMORY
					}
				])
			},
			widgetGroup {
				name "Num Of Active Threads"
				widgets ([
					balanceGauge{metric = "Num Of Active Threads"},
					barLineChart{
						metric "Num Of Active Threads"
						axisYUnit Unit.REGULAR
					}
				])
			}     ,
			widgetGroup {

				name "Current Http Threads Busy"
				widgets([
					balanceGauge{metric = "Current Http Threads Busy"},
					barLineChart {
						metric "Current Http Threads Busy"
						axisYUnit Unit.REGULAR
					}
				])
			} ,
			widgetGroup {

				name "Current Http Threads Count"
				widgets([
					balanceGauge{metric = "Current Http Thread Count"},
					barLineChart {
						metric "Current Http Thread Count"
						axisYUnit Unit.REGULAR
					}
				])
			} ,
			widgetGroup {

				name "Request Backlog"
				widgets([
					balanceGauge{metric = "Backlog"},
					barLineChart {
						metric "Backlog"
						axisYUnit Unit.REGULAR
					}
				])
			}  ,
			widgetGroup {
				name "Active Sessions"
				widgets([
					balanceGauge{metric = "Active Sessions"},
					barLineChart {
						metric "Active Sessions"
						axisYUnit Unit.REGULAR
					}
				])
			},
			widgetGroup {
				name "Total Requests Count"
				widgets([
					balanceGauge{metric = "Total Requests Count"},
					barLineChart {
						metric "Total Requests Count"
						axisYUnit Unit.REGULAR
					}
				])
			}
		]
		)
	}
	
	network {
        port = currHttpPort
        protocolDescription ="HTTP"
    }
	
	scaleCooldownInSeconds 20
	samplingPeriodInSeconds 1

	// Defines an automatic scaling rule based on "counter" metric value
	scalingRules ([
		scalingRule {

			serviceStatistics {
				metric "Total Requests Count"
				statistics Statistics.maximumThroughput
				movingTimeRangeInSeconds 20
			}

			highThreshold {
				value 1
				instancesIncrease 1
			}

			lowThreshold {
				value 0.2
				instancesDecrease 1
			}
		}
	])
}