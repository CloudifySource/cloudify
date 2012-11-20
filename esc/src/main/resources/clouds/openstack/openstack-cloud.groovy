
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
			
		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
		// Therefore, if setting a custom URL, make sure to leave out the suffix.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.3.0-M2/gigaspaces-cloudify-2.3.0-m2-b3482.zip"
		
		machineNamePrefix "cloudify_agent_"
		
		
		managementOnlyFiles ([])
		
		managementGroup "cloudify_manager"
		numberOfManagementMachines 1
		
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
		
	}
	user {
		user user
		apiKey apiKey
		
	}
	templates ([
				SMALL_LINUX : template{
					username "root"
					imageId "221"
					machineMemoryMB 1600
					hardwareId "102"
					remoteDirectory "/root/gs-files"
					localDirectory "upload"
					keyFile keyFile
					
					options ([
						"openstack.securityGroup" : "test",
						"openstack.keyPair" : keyPair
					])
					
					// enable sudo.
					privileged true

					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://az-2.region-a.geo-1.compute.hpcloudsvc.com/",
		"openstack.identity.endpoint": "https://region-a.geo-1.identity.hpcloudsvc.com:35357/",
		"openstack.tenant" : tenant,
		"openstack.wireLog": "false"

	])
}

