@Grab(group='redis.clients', module='jedis', version='2.0.0')
import redis.clients.jedis.Jedis

service {

	name "voltdb"
	type "NOSQL_DB"
	icon "voltdb.jpg"

	lifecycle{
		init "voltdb_install.groovy"
	    start "voltdb_start.groovy"
	}
	plugins([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [6379],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
	])


	userInterface {
		metricGroups = ([
			metricGroup {

				name "voltdb"

				metrics([
					"keyspace hits", "keyspace misses",
				])
			},
		]
		)

	/*	widgetGroups = ([
			widgetGroup {
				name "keyspace hits"
				widgets ([
					barLineChart{
						metric "keyspace hits"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
		]
		)*/
	}
}


