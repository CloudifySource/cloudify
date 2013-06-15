
service {
	
	name "simple"
	
	type "WEB_SERVER"
	numInstances 1
	compute {
		//the templeate name is reference to template definition in the cloud driver
		template "BIG_LINUX_32"
	
	}
	lifecycle {

		start (["Win.*":"run.bat", "Linux":"run.sh", "Mac.*":"run.sh"])

		stopDetection "stopDetecion.sh"
	}

	
	
}