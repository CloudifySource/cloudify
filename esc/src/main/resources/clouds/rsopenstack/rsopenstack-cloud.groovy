
cloud {
	name = "rackspace"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.openstack.RSCloudDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp true
	}

	provider {
		provider "rackspace"
		localDirectory "tools/cli/plugins/esc/rsopenstack/upload"
		remoteDirectory "/root/gs-files"
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-m2-b1193.zip" 
		machineNamePrefix "agent"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "management"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "gsrackspace"
		apiKey "e844c76f985f2e00d7278f51c0ef1333"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "118"
					machineMemoryMB 1600
					hardwareId "4"
					
				}
			])
			
	custom ([
		"openstack.identity.endpoint": "https://auth.api.rackspacecloud.com/",
		"openstack.wireLog": "false"

	])
}

