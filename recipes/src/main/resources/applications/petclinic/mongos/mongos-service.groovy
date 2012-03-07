service {
	
	name "mongos"
	icon "mongodb.png"
	type "NOSQL_DB"
	numInstances 1
	
	compute {
		template "SMALL_LINUX"
	}

	lifecycle {
		install "mongos_install.groovy"
		start "mongos_start.groovy"		
		postStart "mongos_poststart.groovy"
		startDetectionTimeoutSecs 240
		startDetection {
			ServiceUtils.isPortOccupied(context.attributes.thisInstance["port"])
		}
	}
	
	plugins([		        
		plugin {
			name "MongoDBMonitorsPlugin"
			className "org.cloudifysource.mongodb.MongoDBMonitorsPlugin"
			config([				
				"host":"127.0.0.1",				
				"dbName":"admin", 
				"dataSpec":([				    
					"Current Active Connections":"connections.current"					
				])
			])
		}
		
	])

	userInterface {
		metricGroups = ([
			metricGroup {
				name "MongoDB"
				metrics([					
					"Current Active Connections"					
				])
			}
		])

		widgetGroups = ([			
			widgetGroup {
				name "Current Active Connections"
				widgets ([
					balanceGauge{metric = "Current Active Connections"},
					barLineChart{
						metric "Current Active Connections"
						axisYUnit Unit.REGULAR
					},
				])
			}			
		])
	}  
}
