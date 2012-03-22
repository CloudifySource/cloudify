
cloud {
	name = "byon"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp true
		bootstrapManagementOnPublicIp false
		remoteUsername "ENTER_CLOUD_USER"
		remotePassword "ENTER_CLOUD_PASSWORD"
	}

	provider {
		provider "byon"
		localDirectory "tools/cli/plugins/esc/byon/upload"
		remoteDirectory "/tmp/gs-files"
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-m4-b1194-69.zip"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		

		sshLoggingLevel "WARNING"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
	}
	
	user {
		keyFile ""
	}
	
	templates ([
				SMALL_LINUX : template{
					machineMemoryMB 1600
					custom ([
						"nodesList" : ([
										([
											"id" : "byon-pc-lab",
											"ip" : "0.0.0.0"
										])
						])
					])
				}
	])

}

