service {
	
	icon "gigaspaces_logo.gif"
	name "stockAnalyticsFeeder"
	statefulProcessingUnit {
		binaries "stockAnalyticsFeeder.jar" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 128
				maxMemoryCapacity 128
				memoryCapacity 128
			}
	}
	
}
