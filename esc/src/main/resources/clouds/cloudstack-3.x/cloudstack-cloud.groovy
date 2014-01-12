cloud {
	name = "cloudstack"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {

		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		//bootstrapManagementOnPublicIp true
		// Path to folder where management state will be written.
		persistentStoragePath persistencePath
	}

	/*************
	 * Provider specific information.
	 */
	provider {

		provider "cloudstack"

		//The URL for the cloudify ditribution that will be downloaded when new image are created. 
		//only use this if you want to override the default cloudify URL
		//cloudifyUrl cloudifyUrl

		machineNamePrefix "cloudify-agent-"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.
		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "WARNING"

		// Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
		managementGroup "cloudify-manager-"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1

		reservedMemoryCapacityPerMachineInMB 448 

	}

	/*************
	 * Cloud authentication information
	 */
	user {
		apiKey secretKey
		user apiKey
	}

	cloudCompute {
		/***********
		 * Cloud machine templates available with this cloud.
		 */
		templates ([

			SMALL_LINUX : computeTemplate{
				locationId zoneId
				// Mandatory. Image ID.
				imageId templateId
				// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
				remoteDirectory remoteUploadDirectory
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory localUploadDirectory
				// Mandatory. Amount of RAM available to machine.
				machineMemoryMB 1600 
				// Mandatory. Hardware ID.
				hardwareId computeOfferingId
				
				//The SSH username and password for Cloudify to use when installing and configuring created VMs. 
				//This configuration assumes you're using simple SSH authentication without a keypair. 
				//If you're using keypair to authenticate comment out the below two lines. 
				//username sshUsername
				//password sshPassword

				//Optional. Name of key file to use for authenticating to the remote machine. Uncomment this line 
				//if key files are to be used for SSH authentication into the created VM. 
				//keyFile sshKeypairFile
				
				javaUrl "http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-x64.bin"
				
				privileged true

				overrides ([
					"securityGroups" : [securityGroup] as String[],
					//"diskOfferingsId":[diskOfferingsId],
					"jclouds.endpoint" : cloudStackAPIEndpoint
				])
				options ([

					"networkIds" : Arrays.asList([publicNetworkId, privateNetworkId] as String[]),
					"setupStaticNat" : false, 
					//Uncomment this line if key files are to be for SSH authentication into the created VM. 
					//"keyPair" : sshKeypairName

				])
				installer {
					connectionTestRouteResolutionTimeoutMillis 120000
					connectionTestIntervalMillis 5000
					connectionTestConnectTimeoutMillis 10000

					fileTransferConnectionTimeoutMillis 10000
					fileTransferRetries 2
					//fileTransferPort -1
					fileTransferConnectionRetryIntervalMillis 5000

					//remoteExecutionPort -1
					remoteExecutionConnectionTimeoutMillis 10000
				}
			}
		])
	}
}

