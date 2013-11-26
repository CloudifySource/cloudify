/***************
 * Cloud configuration file for the Openstack cloud. *
 */
cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "openstack"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver"
		networkDriverClassName "org.cloudifysource.esc.driver.provisioning.network.openstack.OpenstackNetworkDriver"
		
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "MEDIUM_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
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
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.0-SNAPSHOT/gigaspaces-cloudify-2.7.0-m7-b5991-141"

		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify-agent-"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.

		//
		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "WARNING"

		// Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
		managementGroup "cloudify-manager-"
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
	 * Cloud networking configuration.
	 */
	cloudNetwork {

		// Details of the management network, which is shared among all instances of the Cloudify Cluster.
		management {
			networkConfiguration {
				// The network name
				name  "Cloudify-Management-Network"

				// Subnets
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
				subnets {
					subnet {
						name "Cloudify-Application-Subnet"
						range "160.0.0.0/24"
						options { gateway "null" }
					}
					subnet {
						range "160.1.0.0/24"
						options ([ "gateway" : "null" ])
					}
				}

				custom ([ "associateFloatingIpOnBootstrap" : "true" ])
			}
		])

		custom ([ "global.network.custom.setting" : "Some other other value" ])
	}
	
	cloudCompute {
		
		/***********
		 * Cloud machine templates available with this cloud.
		 */
		templates ([
					MEDIUM_LINUX : computeTemplate{
						// Mandatory. Image ID.
						imageId imageId
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory remoteDirectory
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1600
						// Mandatory. Hardware ID.
						hardwareId hardwareId
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
						// Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
						// are not used.
						keyFile keyFile
						// file transfer protocol
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
	
						username "root"
						// Additional template options.
						// When used with the default driver, the option names are considered
						// method names invoked on the TemplateOptions object with the value as the parameter.
						options ([
									"networkServiceName" : "neutron", // optional property (default: neutron) 
									"networkApiVersion" : "v2.0", // optional property (default: v2.0)
									"keyPairName" : keyPair
								])
	
						// Optional. Overrides to default cloud driver behavior.
						// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
						overrides ([
							"jclouds.endpoint": openstackUrl
						])
	
						// enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						// initializationCommand "echo Cloudify agent is about to start"
					}
				])
	
	}

	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones.
	 */
	custom ([:])
}