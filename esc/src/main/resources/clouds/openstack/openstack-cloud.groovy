
cloud {
	name = "hp"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.openstack.OpenstackCloudDriver"
		managementMachineTemplate "MEDIUM_LINUX_64"
		connectToPrivateIp true
	}

	provider {
		provider "hp"
		localDirectory "tools/cli/plugins/esc/hp/upload"
		remoteDirectory "/root/gs-files"
		cloudifyUrl "http://s3.amazonaws.com/gigaspaces-cloudify/cloudify/hp/gigaspaces-hp.zip" 
		machineNamePrefix "agent"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "management"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "ENTER_USER"
		apiKey "ENTER_KEY"
		keyFile "ENTER_KEY_FILE"
	}
	templates ([
				MEDIUM_LINUX_64 : template{
					imageId "221"
					machineMemoryMB 1600
					hardwareId "102"
					//locationId "us-east-1"
					options ([
						"openstack.securityGroup" : "test",
						"openstack.keyPair" : "hp-cloud-demo",
						// indicates if a floating IP should be assigned to this machine. Defaults to true.
						"openstack.allocate-floating-ip" : "true"
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

