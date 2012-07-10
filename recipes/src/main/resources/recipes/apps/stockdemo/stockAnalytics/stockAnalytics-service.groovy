service {
	numInstances 1
	icon "gigaspaces_logo.gif"
	name "stockAnalytics"
	type "UNDEFINED"
	statelessProcessingUnit {
		binaries "stockAnalytics.jar" //can be a folder, or a war file
		sla { memoryCapacityPerContainer 256 }
	}

}