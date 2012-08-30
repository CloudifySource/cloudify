application {
	name="petclinic"
	
	service {
		name = "mongod"		
	}
	
	service {
		name = "mongoConfig"		
	}
	
	service {
		name = "apacheLB"		
	}
	
	service {
		name = "mongos"
		dependsOn = ["mongoConfig", "mongod"]
	}
	
	service {
		name = "tomcat"
		dependsOn = ["mongos","apacheLB"]
	}
}