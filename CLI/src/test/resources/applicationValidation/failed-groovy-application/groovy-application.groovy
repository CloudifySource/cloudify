
application {
	
	name="groovy"
	
	service {
		name = "groovy1"	
	}
	
	service {
		name = "groovy2"
		dependsOn = ["groovy1"]
	}
	
}