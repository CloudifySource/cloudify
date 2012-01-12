service {
	
	name "mongos"
	icon "mongodb.png"
	type "NOSQL_DB"
	numInstances 1

	lifecycle {
		init "mongos_install.groovy"
		start "mongos_start.groovy"		
		postStart "mongos_poststart.groovy"
	}
	
	plugins([		
        plugin {
            name "portLiveness"
            className "org.cloudifysource.mongodb.MongoLivenessDetector"
            config ([                
				"port" : 30000,
                "timeoutInSeconds" : 240,
                "host" : "127.0.0.1"
            ])
        }, 
		plugin {
			name "MongoDBMonitorsPlugin"
			className "org.cloudifysource.mongodb.MongoDBMonitorsPlugin"
			config([				
				"host":"127.0.0.1",
				"port" : 30000,
				"dbName":"mydb", 
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
