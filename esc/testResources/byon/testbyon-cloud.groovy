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
		// Optional. Cloud-generic credentials. Can be overridden by specific credentials on each node, in the templates section.
		remoteUsername "tgrid"
		remotePassword "tgrid"
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		provider "byon"
		
		// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.  
		localDirectory "tools/cli/plugins/esc/byon/upload"
		// Mandatory. Files from the local directory will be copied to this directory on the remote machine. 
		remoteDirectory "/tmp/gs-files"
		// Mandatory. The HTTP/S URL where cloudify can be downloaded from by newly started machines.
		cloudifyUrl "http://pc-lab25:8087/publish/gigaspaces.zip"
		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify_agent_"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "INFO"
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
		// Optional. Key file used to access the cloud.
		keyFile ""
	}
	
	/***********
	 * Cloud machine templates available with this cloud. 
	 */
	templates ([
				SMALL_LINUX : template{
					custom ([
						"nodesList" : ([
										([
											"id" : "byon-test01",
											"ip" : "0.0.0.1",
											"username" : "tgrid1",
											"credential" : "tgrid1"
										]),
										([
											"id" : "byon-test02",
											"ip" : "0.0.0.2"
										]),
										([
											"id" : "byon-test1",
											"ip" : "pc-lab39,pc-lab40,0.0.0.5"
										]),
										([
											"id" : "byon-test2{0}",
											"ip" : "0.0.0.6,0.0.0.7,0.0.0.8"
										]),
										([
											"id" : "byon-test3{0}",
											"ip" : "0.0.0.9-0.0.0.11"
										]),
										([
											"id" : "byon-test4{0}",
											"ip" : "0.0.0.12/31"
										])
						])
					])
				}
	])
	
	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones. 
	 */
	 // Optional. Sets whether to delete the remoteDirectory created by the cloud driver, when shutting down.
	custom ([
		"cleanGsFilesOnShutdown": "false"
	])

}