service {
  numInstances 1
	icon "icon.png"
	name "stockAnalytics"
	statelessProcessingUnit {
		binaries "stockAnalytics.jar" //can be a folder, or a war file   		
		sla {
				memoryCapacityPerContainer 256
			}
	}
	
}