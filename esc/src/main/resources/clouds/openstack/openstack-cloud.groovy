
cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "Openstack"
	configuration {
		// Mandatory - openstack Diablo cloud driver.
		className "org.cloudifysource.esc.driver.provisioning.openstack.OpenstackCloudDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
	}

	provider {
		// optional 
		provider "openstack"
		localDirectory "tools/cli/plugins/esc/openstack/upload"
		remoteDirectory "/root/gs-files"
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-ga-b1196-185.zip" 
		machineNamePrefix "cloudify_agent_"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "cloudify_manager"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
		
	}
	user {
		user "ENTER_USER"
		apiKey "ENTER_API_KEY"
		keyFile "ENTER_KEY_FILE"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "221"
					machineMemoryMB 1600
					hardwareId "102"
										
					options ([
						"openstack.securityGroup" : "test",
						"openstack.keyPair" : "hp-cloud-demo"
					])
					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://az-2.region-a.geo-1.compute.hpcloudsvc.com/",
		"openstack.identity.endpoint": "https://region-a.geo-1.identity.hpcloudsvc.com:35357/",
		"openstack.tenant" : "ENTER_TENANT",
		"openstack.wireLog": "false"

	])
}

