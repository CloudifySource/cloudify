
service {

    name "iisproxy" 
    numInstances 2
    maxAllowedInstances 2
    lifecycle load("lifecycle.groovy")
    userInterface load("ui.groovy")
    
	customCommands ([
		"custom_command" : { 
			name,port ->
        pattern = "^${name}/(.*)"	
		}
	])
      
}