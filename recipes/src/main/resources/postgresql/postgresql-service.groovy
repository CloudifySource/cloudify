service {

	name "postgresql"
	type "DATABASE"
	icon "http://www.postgresql.org/files/community/propaganda/32x32_1.gif"

	lifecycle{
		init "postgresql_install.groovy"
		start "postgresql_start.groovy"
		preStop "postgresql_stop.groovy"
	}

	plugins([
		plugin {
			name "portLiveness"
			className "com.gigaspaces.cloudify.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [5432],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
	])

}
