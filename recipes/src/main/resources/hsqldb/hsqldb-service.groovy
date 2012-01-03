service {

	name "hsqldb"
	type "DATABASE"
	icon "hypersql_logo.png"

	lifecycle{
		init "hsqldb_install.groovy"
		start "hsqldb_start.groovy"
		preStop "hsqldb_stop.groovy"
	}

	plugins([
		plugin {
			name "portLiveness"
			className "com.gigaspaces.cloudify.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [9001],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
	])

}