/***************
 * Cloud configuration file for the Bring-Your-Own-Node (BYON) cloud.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 *
 * @author noak
 *
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "byon"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// The cloud-driver implementation class.
		className "org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		//Indicates whether communications with the management servers should use the machine private IP.
		bootstrapManagementOnPublicIp false
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		provider "byon"


		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.2.0/gigaspaces-cloudify-2.2.0-m4-b2493-70.zip"

		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify_agent_"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.

		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "INFO"
		// Mandatory. Name of the new machine/s started as cloudify management machines.
		managementGroup "cloudify_manager"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1

		reservedMemoryCapacityPerMachineInMB 1024
		reservedMemoryCapacityPerManagementMachineInMB 1280
	}

	/*************
	 * Cloud authentication information
	 */
	user {

	}

	cloudCompute {
		/***********
		 * Cloud machine templates available with this cloud. 
		 */
		templates ([
					// Mandatory. Template Name.
					SMALL_LINUX : computeTemplate{
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 5850
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/tmp/gs-files"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
					
						// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
						username "username"
						password "password"

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Mandatory for BYON.
						custom ([
									// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
									// For each node required:
									// An alias for this node. can be static or use a template with an dynamic-index.
									// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
									//Optional - user and password for the node. can be taken from general cloud configuration.
									"nodesList" : ([
										([
											"id" : "byon-pc-lab{0}",
											"host-list" : "SMALL_LINUX_HOSTS"
										])
									])
								])

						// enable sudo.
						privileged true

						env ([
									"GIGASPACES_TEST_ENV" : "myEnvVariable"
								])

					},
					// Mandatory. Template Name.
					TEMPLATE_1 : computeTemplate{
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 5850
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/tmp/gs-files"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
						
						// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
						username "username"
						password "password"

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Mandatory for BYON.
						custom ([
									// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
									// For each node required:
									// An alias for this node. can be static or use a template with an dynamic-index.
									// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
									//Optional - user and password for the node. can be taken from general cloud configuration.
									"nodesList" : ([
										([
											"id" : "byon-pc-lab{0}",
											"host-list" : "TEMPLATE_1_HOSTS"
										])
									])
								])

						// enable sudo.
						privileged true

						env ([
									"GIGASPACES_TEST_ENV" : "myEnvVariable"
								])


					},
					// Mandatory. Template Name.
					TEMPLATE_2 : computeTemplate{
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 5850
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/tmp/gs-files"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
						
						// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
						username "username"
						password "password"

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Mandatory for BYON.
						custom ([
									// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
									// For each node required:
									// An alias for this node. can be static or use a template with an dynamic-index.
									// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
									//Optional - user and password for the node. can be taken from general cloud configuration.
									"nodesList" : ([
										([
											"id" : "byon-pc-lab{0}",
											"host-list" : "TEMPLATE_2_HOSTS"
										])
									])
								])

						// enable sudo.
						privileged true

						env ([
									"GIGASPACES_TEST_ENV" : "myEnvVariable"
								])


					},
					// Mandatory. Template Name.
					TEMPLATE_3 : computeTemplate{
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 5850
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/tmp/gs-files"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP
						
						// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
						username "username"
						password "password"

						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"

						// Mandatory for BYON.
						custom ([
									// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
									// For each node required:
									// An alias for this node. can be static or use a template with an dynamic-index.
									// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
									//Optional - user and password for the node. can be taken from general cloud configuration.
									"nodesList" : ([
										([
											"id" : "byon-pc-lab{0}",
											"host-list" : "TEMPLATE_3_HOSTS"
										])
									])
								])

						// enable sudo.
						privileged true

						env ([
									"GIGASPACES_TEST_ENV" : "myEnvVariable"
								])

					}
				])
	}

	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones. 
	 */
	// Optional. Sets whether to delete the remoteDirectory created by the cloud driver, when shutting down.
	custom ([
				"cleanGsFilesOnShutdown": "true",
				"org.cloudifysource.clearRemoteDirectoryOnStart":"false",
				"itemsToClean": ([
					"/tmp/gs-files/gigaspaces.tar.gz"
				])
			])

}