cloud {
	name = "openstack"

	configuration {  managementMachineTemplate "MANAGER" }

	provider {
		provider "openstack-nova"
		managementGroup "cloudify-manager-"
		machineNamePrefix "cloudify-agent-"
		numberOfManagementMachines 1
	}
	user {
		user "tenant:user"
		apiKey "apiKey"
	}

	cloudCompute {
		templates ([
			MANAGER : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				computeNetwork {
					networks ([
						"SOME_INTERNAL_NETWORK_1",
						"SOME_INTERNAL_NETWORK_2"
					])
				}
		
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
				
			},
			APPLI : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
			},
			APPLI2 : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
			},
			APPLI3 : computeTemplate{
				imageId "region/imageId"
				remoteDirectory "remoteDirectory"
				machineMemoryMB 1600
				hardwareId "region/hardwareId"
				localDirectory "upload"
				computeNetwork {
					networks ([
						"SOME_INTERNAL_NETWORK_2"
					])
				}
				
				overrides ([
					"openstack.endpoint": "openstackUrl"
				])
			}
		])
	}
}