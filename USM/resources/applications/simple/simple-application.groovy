
application {
	name="simple"


	service {
		name = "simple"
		type = "hello" 
	}

	service {
		
		name = "simple2"
		dependsOn = ["simple"]
	}
}