service {
	
	icon "gigaspaces_logo.gif"
	name "StockDemo"
	statelessProcessingUnit {
		binaries "stockDemo.war" //can be a folder, or a war file   		
		sla {
				highlyAvailable false
				memoryCapacityPerContainer 128 
			}
	}
	
    // TODO use this value by ServiceController to inject the proper port to jetty
    network {
        port = 8080
        protocolDescription = "HTTP"
    }
    
}
