service {

	name "nginx"
	icon "http://wiki.nginx.org/local/nginx-logo.png"
	type "WEB_SERVER"

	lifecycle{
		init "nginx_install.groovy"
		start "nginx_start.groovy"
		preStop "nginx_stop.groovy"
	}

	//	startDetection {
	//		USMUtils.checkPortsOpen ([8080], "127.0.0.1", 60)
	//	}

	plugins([
		plugin {
			name "portLiveness"
			className "com.gigaspaces.cloudify.usm.liveness.PortLivenessDetector"
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
