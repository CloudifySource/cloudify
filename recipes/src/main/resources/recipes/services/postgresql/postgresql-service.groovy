service {

	name "postgresql"
	type "DATABASE"
	icon "postgres.gif"

	lifecycle{
		init "postgresql_install.groovy"
		start "postgresql_start.groovy"
		preStop "postgresql_stop.groovy"
	}

	plugins([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [5432],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
	])

}
