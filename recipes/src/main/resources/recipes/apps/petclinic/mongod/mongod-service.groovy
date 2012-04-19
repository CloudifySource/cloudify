service {
	
	name "mongod"
	icon "mongodb.png"
	type "NOSQL_DB"
	numInstances 2
	
	compute {
		template "SMALL_LINUX"
	}

	lifecycle {
		install "mongod_install.groovy"
		start "mongod_start.groovy"			
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
				"dbName":"mydb",
				"dataSpec" : [
				    "Active Read Clients":"globalLock.activeClients.readers", 
					"Active Write Clients":"globalLock.activeClients.writers", 
					"Read Clients Waiting":"globalLock.currentQueue.readers", 
					"Write Clients Waiting":"globalLock.currentQueue.writers", 
					"Current Active Connections":"connections.current",
					"Open Cursors":"cursors.totalOpen"
				]
			])
		}
		
	])

	userInterface {
		metricGroups = ([
			metricGroup {
				name "MongoDB"
				metrics([
					"Open Cursors", 
					"Current Active Connections",
					"Active Read Clients",
					"Active Write Clients",
					"Read Clients Waiting",
					"Write Clients Waiting"					
				])
			}
		])

		widgetGroups = ([
			widgetGroup {
				name "Open Cursors"
				widgets ([
					balanceGauge{metric = "Open Cursors"},
					barLineChart{
						metric "Open Cursors"
						axisYUnit Unit.REGULAR
					},
				])
			}, 
			widgetGroup {
				name "Current Active Connections"
				widgets ([
					balanceGauge{metric = "Current Active Connections"},
					barLineChart{
						metric "Current Active Connections"
						axisYUnit Unit.REGULAR
					},
				])
			}, 			
			widgetGroup {
				name "Active Read Clients"
				widgets ([
					balanceGauge{metric = "Active Read Clients"},
					barLineChart{
						metric "Active Read Clients"
						axisYUnit Unit.REGULAR
					},
				])
			}, 
			widgetGroup {
				name "Active Write Clients"
				widgets ([
					balanceGauge{metric = "Active Write Clients"},
					barLineChart{
						metric "Active Write Clients"
						axisYUnit Unit.REGULAR
					},
				])
			}, 
			widgetGroup {
				name "Read Clients Waiting"
				widgets ([
					balanceGauge{metric = "Read Clients Waiting"},
					barLineChart{
						metric "Read Clients Waiting"
						axisYUnit Unit.REGULAR
					},
				])
			}, 
		    widgetGroup {
				name "Write Clients Waiting"
				widgets ([
					balanceGauge{metric = "Write Clients Waiting"},
					barLineChart{
							metric "Write Clients Waiting"
							axisYUnit Unit.REGULAR
					},
				])
			}    
		])
	}  
}
