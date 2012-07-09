service {

    name "iisproxy" 
	type "WEB_SERVER"
    numInstances 2
    maxAllowedInstances 2
    lifecycle {
        init "iisproxy_install.groovy"
        start "iisproxy_start.groovy"
    }
    
    
	customCommands ([
		"custom_command" : { 
			name,port ->
			pattern = "^${name}/(.*)"	
		}
	])
      
}