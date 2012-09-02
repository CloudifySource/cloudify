application {
	name="petclinic"

	service {
		name = "mongod"		
	}
	
	service {
		name = "tomcat"
		dependsOn = ["mongod"]
	}
}