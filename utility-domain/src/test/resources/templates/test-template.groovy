[
				TEMPLATE_NAME : computeTemplate{
				
				machineMemoryMB 1600
				remoteDirectory "/tmp/gs-files"
				username "tgrid"
				password "tgrid"
				
				localDirectory "."
				
				custom ([
					"nodesList" : ([
									([
										"id" : "1",
										"host-list" : "192.168.9.101"
									])
					])
				])
				
				// enable sudo.
				privileged false
				
				}
]