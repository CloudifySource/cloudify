

service {

	name "jboss-service"
	//icon "jboss.jpg"
	type "APP_SERVER"

	lifecycle{
		init "jboss_install.groovy"
		start "jboss_start.groovy"
		preStop "jboss_stop.groovy"
	}

	plugins([
		plugin {
			name "portLiveness"
			className "com.gigaspaces.cloudify.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [9999],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
//		plugin {
//			name "jmx"
//			className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"
//			config([
//						"Current Http Threads Busy":[
//							"jboss.web:type=ThreadPool,name=http-127.0.0.1-8180",
//							"currentThreadsBusy"
//						],
//						"Current Http Threads Count":[
//							"jboss.web:type=ThreadPool,name=http-127.0.0.1-8180",
//							"currentThreadCount"
//						],
//						"Server Thread Count":[
//							"jboss.system:type=ServerInfo",
//							"ActiveThreadCount"
//						],
//						"Free Memory":[
//							"jboss.system:type=ServerInfo",
//							"FreeMemory"
//						],
//						port: 1190
//					])
//		},
//		plugin {
//			name "details"
//			className "com.gigaspaces.cloudify.usm.jmx.JmxDetails"
//			config([
//						"Version":[
//							"jboss.system:type=Server",
//							"Version"
//						],
//						"Bind Address":[
//							"jboss.system:type=ServerConfig",
//							"BindAddress"
//						],
//						port: 1190
//					])
//		}
	])

//	userInterface {
//		metricGroups = ([
//			metricGroup {
//				name "http"
//				metrics([
//					"Current Http Threads Busy",
//					"Current Http Threads Count"
//				])
//			},
//			metricGroup {
//				name "server"
//				metrics([
//					"Server Thread Count",
//					"Free Memory"
//				])
//			}
//		])
//
//		widgetGroups = ([
//			widgetGroup {
//				name "Http Threads Busy"
//				widgets ([
//					barLineChart{
//						metric "Current Http Threads Busy"
//						axisYUnit nit.REGULAR
//					},
//				])
//			}   ,
//			widgetGroup {
//				name "Http Thread Count"
//				widgets ([
//					barLineChart {
//						metric "Current Http Threads Count"
//						axisYUnit nit.REGULAR
//					}
//				])
//			},
//			widgetGroup {
//				name "Server Thread Count"
//				widgets ([
//					barLineChart {
//						metric "Server Thread Count"
//						axisYUnit nit.REGULAR
//					}
//				])
//			},
//			widgetGroup {
//				name "Free Memory"
//				widgets ([
//					barLineChart {
//						metric "Free Memory"
//						axisYUnit Unit.MEMORY
//					}
//				])
//			}
//		])
//	}
}
