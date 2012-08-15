/***************
 * Cloud configuration file for the Amazon ec2 cloud. Uses the default jclouds-based cloud driver.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 * @author barakme
 *
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "ec2"
	
	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "MEDIUM_WIN"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		// When using the default cloud driver, maps to the Compute Service Context provider name.
		provider "aws-ec2"
		
		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.2.0/gigaspaces-cloudify-2.2.0-m3-b2491-71.zip"

		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify_agent_"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.
		dedicatedManagementMachines true
		
		// 
		managementOnlyFiles ([])
		
		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "WARNING"
		
		// Mandatory. Name of the new machine/s started as cloudify management machines. 
		managementGroup "cloudify_manager"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1
		zones (["agent"])
		
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	
	/*************
	 * Cloud authentication information
	 */
	user {
		// Optional. Identity used to access cloud. 
		// When used with the default driver, maps to the identity used to create the ComputeServiceContext.
		user "ENTER_USER"
		
		// Optional. Key used to access cloud.
		// When used with the default driver, maps to the credential used to create the ComputeServiceContext.
		apiKey "ENTER_KEY"
		
		
		
	}
	
	
	/***********
	 * Cloud machine templates available with this cloud. 
	 */
	templates ([
				MEDIUM_WIN : template{
					// Mandatory. Image ID.
					imageId "us-east-1/ami-2b41ef42"
					// Mandatory. Amount of RAM available to machine.
					machineMemoryMB 1600
					// Mandatory. Hardware ID.
					hardwareId "m1.large"
					// Optional. Location ID.
					locationId "us-east-1c"

					// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
					remoteDirectory "/C\$/Users/Administrator/gs-files"
					
					// File transfer mode. Optional, defaults to SCP.
					fileTransfer org.cloudifysource.dsl.cloud.FileTransferModes.CIFS
					// Remote execution mode. Options, defaults to SSH.
					remoteExecution org.cloudifysource.dsl.cloud.RemoteExecutionModes.WINRM
					
					// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
					localDirectory "tools/cli/plugins/esc/ec2-win/upload"
					// Optional. Name of key file to use for authenticating to the remot machine. Remove this line if key files
					// are not used.
					keyFile "ENTER_KEY_FILE"
				
					
					// Additional template options.
					// When used with the default driver, the option names are considered
					// method names invoked on the TemplateOptions object with the value as the parameter.
					options ([
								"securityGroups" : ["default"]as String[],
								"keyPair" : "ENTER_KEY_PAIR_NAME"
							])

					// Optional. Overrides to default cloud driver behavior.
					// When used with the default driver, maps to the overrides properties passed to the ComputeServiceContext a
					overrides (["jclouds.ec2.ami-query":"",
								"jclouds.ec2.cc-ami-query":""])



				}
			])
	
	
	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones. 
	 */
	custom ([:])
}

