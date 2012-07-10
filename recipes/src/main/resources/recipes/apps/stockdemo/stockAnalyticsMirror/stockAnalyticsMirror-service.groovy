service {
	
	icon "gigaspaces_logo.gif"
	name "stockAnalyticsMirror"
	type "UNDEFINED"
	statelessProcessingUnit {
		binaries "stockAnalyticsMirror" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 256 
			}
	}
	
}
