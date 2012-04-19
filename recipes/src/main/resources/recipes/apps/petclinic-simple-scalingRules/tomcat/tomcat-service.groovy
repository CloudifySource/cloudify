import java.util.concurrent.TimeUnit;

service {
	name "tomcat"
	icon "tomcat.gif"
	type "WEB_SERVER"
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
					context.attributes.thisInstance["warUrl"] = "${warUrl}"
					println "tomcat-service.groovy(updateWar customCommand): invoking updateWarFile custom command ..."
					context.waitForService(currentServiceName, 60, TimeUnit.SECONDS).invoke("updateWarFile")
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
	
	// global flag that enables changing number of instances for this service
	elastic true

	// the initial number of instances
	numInstances 1

	// The minimum number of service instances
	// Used together with scaling rules
	minAllowedInstances 1

	// The maximum number of service instances
	// Used together with scaling rules
	maxAllowedInstances 2

	// The time (in seconds) that scaling rules are disabled after scale in (instances removed)
	// and scale out (instances added)
	//
	// This has the same effect as setting scaleInCooldownInSeconds and scaleOutCooldownInSeconds separately.
	//
	// Used together with scaling rules
	scaleCooldownInSeconds 20


	// The time (in seconds) between two consecutive metric samples
	// Used together with scaling rules
	samplingPeriodInSeconds 1

	// Defines an automatic scaling rule based on "counter" metric value
	scalingRules ([
		scalingRule {

			serviceStatistics {

        // The name of the metric that is the basis for the scale rule decision
				metric "Total Requests Count"

        // (Optional)
        // The sliding time range (in seconds) for aggregating per-instance metric samples
        // The number of samples in the time windows equals the time window divided by the sampling period
        // Default: 300
				movingTimeRangeInSeconds 20

        // (Optional)
        // The algorithms for aggregating metric samples by instances and by time.
        // Metric samples are aggregated separately per instance in the specified time range,
        // and then aggregated again for all instances.
        // Default: Statistics.averageOfAverages
        // Possible values: Statistics.maximumOfAverages, Statistics.minimumOfAverages, Statistics.averageOfAverages, Statistics.percentileOfAverages(90)
        //                  Statistics.maximumOfMaximums, Statistics.minimumOfMinimums, Statistics.maximumThroughput
        //
        // This has the same effect as setting instancesStatistics and timeStatistics separately. 
        // For example: 
        // statistics Statistics.maximumOfAverages
        // is the same as:
        // timeStatistics Statistics.average
        // instancesStatistics Statistics.maximum
        // 
				statistics Statistics.maximumThroughput
			}


			highThreshold {
			
        // The value above which the number of instances is increased			
				value 1
				
				// The number of instances to increase when above threshold
				instancesIncrease 1
			}

			lowThreshold {
			
        // The value below which the number of instances is decreased
				value 0.2
				
				// The number of instances to decrease when below threshold
				instancesDecrease 1
			}
		}
	])
}