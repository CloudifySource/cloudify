import com.mongodb.CommandResult;
import com.mongodb.Mongo;
import com.mongodb.DB;
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
		
		monitors{
			try { 
				port  = context.attributes.thisInstance["port"] as int
				mongo = new Mongo("127.0.0.1", port)			
				db = mongo.getDB("mydb")
														
				result = db.command("serverStatus")
				println "mongod-service.groovy: result is ${result}"	
														
				return [
					"Current Active Connections":result.connections.current					
				]
			}			
			finally {
				if (null!=mongo) mongo.close()
			}					
		}		
	}
	
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
	network {
		port = 30001
		protocolDescription ="HTTP"
	} 
}
