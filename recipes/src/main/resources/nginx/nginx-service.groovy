service {

	name "nginx"
	icon "nginx-logo.png"
	type "WEB_SERVER"

	lifecycle{
		init "nginx_install.groovy"
		start "nginx_start.groovy"
		preStop "nginx_stop.groovy"
	}

	plugins([
		plugin {
			name "portLiveness"
			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [8000],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		}
	])

	userInterface {

		metricGroups = ([
		]
		)

		widgetGroups = ([]
		)
	}
}
