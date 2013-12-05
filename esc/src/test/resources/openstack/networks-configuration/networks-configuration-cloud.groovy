cloud {
	name = "openstack"

	configuration { managementMachineTemplate "MANAGER" }

	cloudNetwork {
		management {
			networkConfiguration {
				name "Cloudify-Management-Network"
				subnets ([
					subnet { name "Cloudify-Management-Subnet" }
				])
			}
		}
		templates ([
			"APPLICATION_NET" : networkConfiguration {
				name "Cloudify-Application-Network"
				subnets { subnet { name "Cloudify-Application-Subnet"
					} }
			}
		])
	}

	cloudCompute {
		templates ([
			MANAGER : computeTemplate{
				imageId "imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "hardwareId"
				localDirectory "upload"
				computeNetwork {
					networks ([
						"SOME_INTERNAL_NETWORK_1",
						"SOME_INTERNAL_NETWORK_2"
					])
				}
			},
			APPLI : computeTemplate{
				imageId "imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "hardwareId"
				localDirectory "upload"
				computeNetwork {
					networks ([
						"SOME_INTERNAL_NETWORK_2"
					])
				}
			}
		])
	}
}