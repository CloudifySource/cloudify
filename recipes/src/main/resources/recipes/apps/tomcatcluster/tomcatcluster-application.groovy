
application {
	name="tomcatcluster"
	
	service {
		name = "sessionSpace"	
	}
	
	service {
		name = "tomcat"
		dependsOn = ["sessionSpace"]
	}
	
}