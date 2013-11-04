
application {
	name="travel"
	
	service {
		name = "mysql"
	}
	
	service {
		name = "tomcat"
		dependsOn = ["mysql"]
	}
	
	
}