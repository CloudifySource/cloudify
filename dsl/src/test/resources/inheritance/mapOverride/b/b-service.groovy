import java.util.concurrent.TimeUnit


service {
	extend "../a"
	name "B"
	
	type "UNDEFINED"
	
	lifecycle { 
			
		start "run.groovy" 
			
		
		
	}

	customCommands ([
		"cmdA" :null
	])

}