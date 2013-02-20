
service {

    name "iisproxy" 
    numInstances 2
    maxAllowedInstances 2
    lifecycle load("lifecycle.groovy")
    userInterface load("ui_MISSING.groovy")
    
	
      
}