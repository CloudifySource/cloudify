service {
	
	icon "gigaspaces_logo.gif"
	name "stockAnalyticsProcessor"
	statefulProcessingUnit {
		binaries "stockAnalyticsProcessor.jar" //can be a folder, or a war file   	
		sla {
				memoryCapacity 512
				maxMemoryCapacity 512
				highlyAvailable true
				memoryCapacityPerContainer 256 
			}
	}
	
}