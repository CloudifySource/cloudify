service {
	
	icon "icon.png"
	name "stockAnalyticsFeeder"
	statefulProcessingUnit {
		binaries "stockAnalyticsFeeder.jar" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 128 
			}
	}
	
}
