service {

  name "iisproxy" 
	type "WEB_SERVER"
	locationAware true
	elastic true
  numInstances 2
  maxAllowedInstances 2
  
}