cloud {
	name = "openstack"

	configuration { managementMachineTemplate "MANAGER" }

	provider {
		provider "openstack-nova"
		managementGroup "cloudify-manager-"
		numberOfManagementMachines 1
	}

	cloudNetwork {
		management {
			networkConfiguration {
				name "Cloudify-Management-Network"
				subnets ([
					subnet { name "Cloudify-Management-Subnet" }
				])
				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
			}
		}
		templates ([
			"APPLICATION_NET" : networkConfiguration {
				name "Cloudify-Application-Network"
				subnets { subnet { name "Cloudify-Application-Subnet"
					} }
				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
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