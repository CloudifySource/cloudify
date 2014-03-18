/***************
 * Cloud configuration file for the hp-havana cloud.
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "hp-havana"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver"
		networkDriverClassName "org.cloudifysource.esc.driver.provisioning.network.openstack.OpenstackNetworkDriver"
		storageClassName "org.cloudifysource.esc.driver.provisioning.storage.openstack.OpenstackStorageDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		
		// Optional. Path to folder where management state will be written. Null indicates state will not be written.
		persistentStoragePath persistencePath
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		// When using the default cloud driver, maps to the Compute Service Context provider name.
		provider "openstack-nova"

		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
		// Therefore, if setting a custom URL, make sure to leave out the suffix.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.1-6203-M3/gigaspaces-cloudify-2.7.1-m3-b6203.zip"

		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify-agent-"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.

		//
		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "WARNING"

		// Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
		managementGroup "cloudify-manager"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1

		reservedMemoryCapacityPerMachineInMB 1024

	}

	/*************
	 * Cloud authentication information
	 */
	user {
		// Optional. Identity used to access cloud.
		// When used with the default driver, maps to the identity used to create the ComputeServiceContext.
		user "${tenant}:${user}"

		// Optional. Key used to access cloud.
		// When used with the default driver, maps to the credential used to create the ComputeServiceContext.
		apiKey apiKey



	}
	
		/********************
		 * Cloud storage configuration.
		 */
		cloudStorage {
				templates ([
					SMALL_BLOCK : storageTemplate{
									deleteOnExit false
									partitioningRequired true
									size 1
									path "/storage"
									namePrefix "cloudify-storage-volume"
									deviceName "/dev/vdc"
									fileSystemType "ext3"
					}
			])
		}

	/********************
	 * Cloud networking configuration.
	 */
	cloudNetwork {

		// Details of the management network, which is shared among all instances of the Cloudify Cluster.
		management {
			networkConfiguration {
				name  "Cloudify-Management-Network"
				subnets ([
					subnet {
						name "Cloudify-Management-Subnet"
						range "177.86.0.0/24"
						options ([ "gateway" : "177.86.0.111" ])
					}
				])
				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
			}
		}

		// Templates for networks which applications may use
		// Only service isntances belonging to an application will be attached to this network.
		templates ([
			"APPLICATION_NET" : networkConfiguration {
				name  "Cloudify-Application-Network"
				subnets ([
					subnet {
						name "Cloudify-Application-Subnet"
						range "160.0.0.0/24"
						options { gateway "null" }
					}
				])
				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
			}
		])
	}

	cloudCompute {

		/***********
		 * Cloud machine templates available with this cloud.
		 */
		templates ([
					// Mandatory. Template Name.
					SMALL_LINUX : computeTemplate{
						// Mandatory. Image ID.
						imageId linuxImageId
						
						// file transfer protocol
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SFTP
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/root/gs-files"

						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1900

						// Mandatory. Hardware ID.
						hardwareId hardwareId

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Optional. Name of key file to use for authenticating to the remote machine.
						// Remove this line if key files are not used.
						keyFile keyFile
	
						// Mandatory. Username to use for authenticating to the remote machine.
						username "root"

						// Additional template options
						options ([// Optional. Set the name to search to find openstack compute endpoint.
							// "computeServiceName" : "nova",
	
							// Optional. Set the name to search to find openstack compute endpoint.
							// "networkServiceName" : "neutron",
		
							// Optional. Set the network api version .
							// "networkApiVersion"  : "v2.0",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// Specify an existing external router name to use.
							// "externalRouterName" : "router-ext",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// Specify an external network name to use.
							// "externalNetworkName": "net-ext",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// By default (if you use a management network), the driver will create a router and link it to an external network, set to 'false' to ignore this step.
							// By setting this property to 'false', the properties 'externalRouterName' and 'externalNetworkName' will be ignored.
							// "skipExternalNetworking": "false",
							
							// Optional. Use existing security groups.
							// "securityGroups" : ["default"] as String[],
							"keyPairName" : keyPair
						])
						
						// Optional. Use existing networks.
						// computeNetwork {
						//	networks (["SOME_INTERNAL_NETWORK"])
						// }
						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true
	
						// Optional. Overrides to default cloud driver behavior.
						// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
						overrides ([
							"openstack.endpoint": openstackUrl
						])
	
						// enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"

						// optional - set a list of availability zones to use
						// availabilityZones (["az1","az2","az3"])
	
					},
					SMALL_UBUNTU : computeTemplate{
						// Mandatory. Image ID.
						imageId ubuntuImageId
						
						// file transfer protocol
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SFTP
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/root/gs-files"

						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1900

						// Mandatory. Hardware ID.
						hardwareId hardwareId

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Optional. Name of key file to use for authenticating to the remote machine.
						// Remove this line if key files are not used.
						keyFile keyFile
	
						// Mandatory. Username to use for authenticating to the remote machine.
						username "root"

						// Additional template options
						options ([// Optional. Set the name to search to find openstack compute endpoint.
							// "computeServiceName" : "nova",
	
							// Optional. Set the name to search to find openstack compute endpoint.
							// "networkServiceName" : "neutron",
		
							// Optional. Set the network api version .
							// "networkApiVersion"  : "v2.0",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// Specify an existing external router name to use.
							// "externalRouterName" : "router-ext",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// Specify an external network name to use.
							// "externalNetworkName": "net-ext",
		
							// Optional. This option is relevant only if you use manamgent network. 
							// By default (if you use a management network), the driver will create a router and link it to an external network, set to 'false' to ignore this step.
							// By setting this property to 'false', the properties 'externalRouterName' and 'externalNetworkName' will be ignored.
							// "skipExternalNetworking": "false",
							
							// Optional. Use existing security groups.
							// "securityGroups" : ["default"] as String[],
							"keyPairName" : keyPair
						])
						
						// Optional. Use existing networks.
						// computeNetwork {
						//	networks (["SOME_INTERNAL_NETWORK"])
						// }
						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true
	
						// Optional. Overrides to default cloud driver behavior.
						// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
						overrides ([
							"openstack.endpoint": openstackUrl
						])
	
						// enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"

						// optional - set a list of availability zones to use
						// availabilityZones (["az1","az2","az3"])
					}
	
	
				])
	
	}


	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones.
	 */
	custom ([:])
}
