
cloud {
	name = "rackspace"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.openstack.RSCloudDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp true
	}

	provider {
		provider "rackspace"
		
		//The machineNamePrefix property may not contain the char '_' in Rackspace
		machineNamePrefix "agent"
		
		
		managementOnlyFiles ([])
		
		//The managementGroup property may not contain the char '_' in Rackspace
		managementGroup "management"
		numberOfManagementMachines 1
		
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
					remoteDirectory "/root/gs-files"
					localDirectory "tools/cli/plugins/esc/rsopenstack/upload"
					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://servers.api.rackspacecloud.com",
		"openstack.identity.endpoint": "https://auth.api.rackspacecloud.com/",
		//The tenant id is referred to as 'cloud account number' in Rackspace
		"openstack.tenant" : "ENTER_TENANT",
		"openstack.wireLog": "false"

	])
}

