service {
	
	icon "gigaspaces_logo.gif"
	name "stockAnalyticsMirror"
	statelessProcessingUnit {
		binaries "stockAnalyticsMirror" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 256 
			}
	}
	
}
