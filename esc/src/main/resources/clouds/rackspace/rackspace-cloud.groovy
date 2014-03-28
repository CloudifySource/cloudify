/***************
 * Cloud configuration file for the Rackspace (v2) cloud. Uses the default jclouds-based cloud driver.
 * See org.cloudifysource.domain.cloud.Cloud for more details.
 * @author noak
 *
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "rackspace"

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
		
		// Optional. Path to folder where management state will be written. Null indicates state will not be written.
		persistentStoragePath persistencePath
	}
	
	

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		// When using the default cloud driver, maps to the Compute Service Context provider name.
		provider "rackspace-cloudservers-us"


		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
		// Therefore, if setting a custom URL, make sure to leave out the suffix.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.0-5998-RC2/gigaspaces-cloudify-2.7.0-rc2-b5998.zip"


		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify-agent"
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
		user "${user}"

		// Optional. Key used to access cloud.
		// When used with the default driver, maps to the credential used to create the ComputeServiceContext.
		apiKey apiKey
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
						locationId locationId
						
						// file transfer protocol
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SFTP
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/home/root/gs-files"
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1600
						// Mandatory. Hardware ID.
						hardwareId hardwareId
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
	
						username "root"
						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true
	
						// Optional. Overrides to default cloud driver behavior.
						// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
						overrides ([
							"jclouds.keystone.credential-type":"RAX-KSKEY:apiKeyCredentials"
						])
						
						// enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						// initializationCommand "echo Cloudify agent is about to start"
					},
					
					// Mandatory. Template Name.
					SMALL_UBUNTU : computeTemplate{
						// Mandatory. Image ID.
						imageId ubuntuImageId
						locationId locationId
						
						// file transfer protocol
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SFTP
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/home/root/gs-files"
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1600
						// Mandatory. Hardware ID.
						hardwareId hardwareId
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
	
						username "root"
						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true
	
						// Optional. Overrides to default cloud driver behavior.
						// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
						overrides ([
							"jclouds.keystone.credential-type":"RAX-KSKEY:apiKeyCredentials"
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
	custom (["org.cloudifysource.clearRemoteDirectoryOnStart":true])
}