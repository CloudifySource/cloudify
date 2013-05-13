service {
	
	name "stateful"
	elastic true
	
	statefulProcessingUnit {
	
		binaries "stateful.jar" //can be a folder, or a war file   
	
		sla {
				memoryCapacity 256
				maxMemoryCapacity 512
				highlyAvailable true
				memoryCapacityPerContainer 128
			}
	
		contextProperties ([
				//this is the usual deployment properties mechanism 
				"cluster-config.mirror-service.interval-opers":"1000"
		])
	}
	
}