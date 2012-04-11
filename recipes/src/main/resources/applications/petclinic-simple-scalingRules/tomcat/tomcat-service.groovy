import java.util.concurrent.TimeUnit;

service {
	name "tomcat"
	icon "tomcat.gif"
	type "WEB_SERVER"
	def portIncrement = {-> context.getInstanceId()-1};
	def readConfig = { ->
		return new ConfigSlurper().parse(new File(context.serviceDirectory.toString(),"tomcat.properties").toURL())
	}
	def getJmxPort = {-> readConfig().jmxPort + portIncrement()}
	def getHttpPort = {-> readConfig().port + portIncrement()}
	def getAjpPort = {-> readConfig().ajpPort + portIncrement()}
	compute {
		template "SMALL_LINUX"
	}

	lifecycle {
		install "tomcat_install.groovy"
		start "tomcat_start.groovy"
		preStop "tomcat_stop.groovy"
		startDetectionTimeoutSecs 240
		startDetection {
			!ServiceUtils.arePortsFree([getHttpPort(), getAjpPort()] )
		}
	}

	customCommands ([
				"updateWar" : {warUrl ->
					println "tomcat-service.groovy(updateWar custom command): warUrl is ${warUrl}..."
					context.attributes.thisInstance["warUrl"] = "${warUrl}"
					println "tomcat-service.groovy(updateWar customCommand): invoking updateWarFile custom command ..."
					context.waitForService("tomcat", 60, TimeUnit.SECONDS).invoke("updateWarFile")
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
							"Catalina:type=ThreadPool,name=\"http-bio-${->getHttpPort()}\"",
							"currentThreadsBusy"
						],
						"Current Http Threads Count": [
							"Catalina:type=ThreadPool,name=\"http-bio-${->getHttpPort()}\"",
							"currentThreadCount"
						],
						"Backlog": [
							"Catalina:type=ProtocolHandler,port=${->getHttpPort()}",
							"backlog"
						],
						"Active Sessions":[
							"Catalina:type=Manager,context=/petclinic-mongo,host=localhost",
							"activeSessions"
						],
						"Total Requests Count": [
							"Catalina:type=GlobalRequestProcessor,name=\"http-bio-${->getHttpPort()}\"",
							"requestCount"
						],
						port: "${->getJmxPort()}"

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

				metric "Total Requests Count"

				movingTimeRangeInSeconds 20

				statistics Statistics.maximumThroughput
			}


			// The instancesStatistics over which the number of instances is increased or decreased
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
