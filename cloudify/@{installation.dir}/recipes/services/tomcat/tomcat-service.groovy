import java.util.concurrent.TimeUnit;
import static JmxMonitors.*

service {
	name "tomcat"
	icon "tomcat.gif"
	type "APP_SERVER"
	
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
	
	
		details {
			def currPublicIP
			
			if (  context.isLocalCloud()  ) {
				currPublicIP =InetAddress.localHost.hostAddress
			}
			else {
				currPublicIP =System.getenv()["CLOUDIFY_AGENT_ENV_PUBLIC_IP"]
			}
			def tomcatURL	= "http://${currPublicIP}:${currHttpPort}"	
			
			def ctxPath = ("default" == context.applicationName)?"":"${context.applicationName}"
			def applicationURL = "${tomcatURL}/${ctxPath}"
			println "tomcat-service.groovy: applicationURL is ${applicationURL}"
		
            return [
                "Application URL":"<a href=\"${applicationURL}\" target=\"_blank\">${applicationURL}</a>"
            ]
		}	

		monitors {
		
			def ctxPath = ("default" == context.applicationName)?"":"${context.applicationName}"
							
			def metricNamesToMBeansNames = [
				"Current Http Threads Busy": ["Catalina:type=ThreadPool,name=\"http-bio-${currHttpPort}\"", "currentThreadsBusy"],				
				"Current Http Thread Count": ["Catalina:type=ThreadPool,name=\"http-bio-${currHttpPort}\"", "currentThreadCount"],				
				"Backlog": ["Catalina:type=ProtocolHandler,port=${currHttpPort}", "backlog"],				
				"Total Requests Count": ["Catalina:type=GlobalRequestProcessor,name=\"http-bio-${currHttpPort}\"", "requestCount"],				
				"Active Sessions": ["Catalina:type=Manager,context=/${ctxPath},host=localhost", "activeSessions"],
			]
			
			return getJmxMetrics("127.0.0.1",currJmxPort,metricNamesToMBeansNames)										
    	}			
	
	
	
		install "tomcat_install.groovy"
		start "tomcat_start.groovy"		
		preStop "tomcat_stop.groovy"
		startDetectionTimeoutSecs 240
		startDetection {
			println "tomcat-service.groovy(startDetection): arePortsFree http=${currHttpPort} ajp=${currAjpPort} ..."
			!ServiceUtils.arePortsFree([currHttpPort, currAjpPort] )
		}
		
		
		def instanceID = context.instanceId
		
		postStart {			
			if ( useLoadBalancer ) { 
				println "tomcat-service.groovy: tomcat Post-start ..."
				def apacheService = context.waitForService("apacheLB", 180, TimeUnit.SECONDS)			
				println "tomcat-service.groovy: invoking add-node of apacheLB ..."
					
				def ctxPath = ("default" == context.applicationName)?"":"${context.applicationName}"				
				
				def privateIP
				if (  context.isLocalCloud()  ) {
					privateIP=InetAddress.getLocalHost().getHostAddress()
				}
				else {
					privateIP =System.getenv()["CLOUDIFY_AGENT_ENV_PRIVATE_IP"]
				}
				println "tomcat-service.groovy: privateIP is ${privateIP} ..."
				
				def currURL="http://${privateIP}:${currHttpPort}/${ctxPath}"
				println "tomcat-service.groovy: About to add ${currURL} to apacheLB ..."
				apacheService.invoke("addNode", currURL as String, instanceID as String)			                 
				println "tomcat-service.groovy: tomcat Post-start ended"
			}			
		}
		
		postStop {
			if ( useLoadBalancer ) { 
				println "tomcat-service.groovy: tomcat Post-stop ..."
				def apacheService = context.waitForService("apacheLB", 180, TimeUnit.SECONDS)			
						
				def ctxPath = ("default" == context.applicationName)?"":"${context.applicationName}"
				println "tomcat-service.groovy: postStop ctxPath is ${ctxPath}"				
				
				def privateIP
				if (  context.isLocalCloud()  ) {
					privateIP=InetAddress.localHost.hostAddress
				}
				else {
					privateIP =System.getenv()["CLOUDIFY_AGENT_ENV_PRIVATE_IP"]
				}				
				
				println "tomcat-service.groovy: privateIP is ${privateIP} ..."
				def currURL="http://${privateIP}:${currHttpPort}/${ctxPath}"
				println "tomcat-service.groovy: About to remove ${currURL} from apacheLB ..."
				apacheService.invoke("removeNode", currURL as String, instanceID as String)
				println "tomcat-service.groovy: tomcat Post-stop ended"
			}			
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

	
	userInterface {

		metricGroups = ([
			metricGroup {

				name "process"

				metrics([
				    "Total Process Cpu Time",
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
			,
			widgetGroup {
				name "Total Process Cpu Time"
				widgets([
					balanceGauge{metric = "Total Process Cpu Time"},
					barLineChart {
						metric "Total Process Cpu Time"
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
				metric "Total Process Cpu Time"
				timeStatistics Statistics.averageCpuPercentage
			    instancesStatistics Statistics.maximum
				movingTimeRangeInSeconds 20
			}

			highThreshold {
				value 40
				instancesIncrease 1
			}

			lowThreshold {
				value 25
				instancesDecrease 1
			}
		}
	])
}