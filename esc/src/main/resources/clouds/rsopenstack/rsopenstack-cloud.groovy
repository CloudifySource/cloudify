
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
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-ga-b1196-161.zip" 
		machineNamePrefix "agent"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "management"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
	}
	user {
		user "USER_NAME"
		apiKey "API_KEY"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "118"
					machineMemoryMB 1600
					hardwareId "4"
					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://servers.api.rackspacecloud.com",
		"openstack.identity.endpoint": "https://auth.api.rackspacecloud.com/",
		"openstack.tenant" : "ENTER_TENANT",
		"openstack.wireLog": "false"

	])
}

