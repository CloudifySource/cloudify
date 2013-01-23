import java.util.concurrent.TimeUnit


service {
	name "A"
	type "UNDEFINED"
	
	lifecycle { 
			
		start "run.groovy" 
			
		
		
	}

	customCommands ([
		"cmdA" :"AAA"
	])

}