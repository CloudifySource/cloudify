service {
	name "test features"
	url "http://" + InetAddress.localHost.hostName + ":8080"
	type "WEB_SERVER"

	lifecycle{

		init "test_parsing_base_install.groovy"

		start ([ "Linux":"tomcat_start.groovy" ,
					"Windows.*": "tomcat_start.groovy"
				])

		postStart {			
			println "post start"			
		}
		
		preStop (["Win.*":"catalina-stop.bat",
					"Linux":"./catalina-stop.sh"])
		
	}
}


