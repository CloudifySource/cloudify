service {
	
	name "solr"
	type "NOSQL_DB"
	icon "solr.png"

	lifecycle{
		install "solr_install.groovy"
		start "solr_start.groovy"
	}
	
	plugins([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [8983],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config([
						"Average Requests PerSecond": [
							"solr/:type=/admin/threads,id=org.apache.solr.handler.admin.ThreadDumpHandler",
							"avgRequestsPerSecond"
						],
						port: 9999
					])
		}
	])

	userInterface {

		metricGroups = ([
			metricGroup {
				name "Requests"
				metrics([
					"Average Requests PerSecond"
				])
			} ,
		]
		)

		widgetGroups = ([
			widgetGroup {
				name "Average Requests PerSecond"
				widgets ([
					barLineChart{
						metric "Average Requests PerSecond"
						axisYUnit Unit.REGULAR
					}
				])
			},
		]
		)
	}
}


