
application {
	name = "travel"
	
	service {
		name = "cassandra"	
	}
	
	service {
		name = "tomcat"
		dependsOn = ["cassandra", "iisproxy"]
	}
	
	service {
		name = "iisproxy"
	}
	
}