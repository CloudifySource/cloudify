application {
	name="petclinic-mongo"
	
	service {
		name = "mongod"		
	}
	
	service {
		name = "mongo-cfg"		
	}
	
	service {
		name = "mongos"
		dependsOn = ["mongo-cfg", "mongod"]
	}
	
	service {
		name = "tomcat"
		dependsOn = ["mongos"]
	}
}