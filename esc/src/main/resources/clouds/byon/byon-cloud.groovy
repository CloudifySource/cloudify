
cloud {
	name = "byon"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver"
		managementMachineTemplate "SMALL_LINUX_32"
		connectToPrivateIp true
		bootstrapManagementOnPublicIp false
	}

	provider {
		provider "byon"
		localDirectory "tools/cli/plugins/esc/byon/upload"
		remoteDirectory "/home/test/"
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-m2-b1193-94.zip"
		machineNamePrefix "cloudify_agent_"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		

		sshLoggingLevel "WARNING"
		managementGroup "cloudify_managemet"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "test"
		apiKey "test"
		keyFile ""
	}
	templates ([
				SMALL_LINUX_32 : template{
					imageId "us-east-1/ami-76f0061f"
					machineMemoryMB 1600
					hardwareId "m1.small"
					locationId "us-east-1"
					options ([
						"securityGroups" : ["default"] as String[],
						"keyPair" : "cloud-demo"
					])
				}
			])
	custom ([
		"nodesPool" : ([
							([
								"id" : "pc-lab42",
								"ip" : "192.168.9.62",
								"username" : "test",
								"credential" : "test"
							]),
							([
								"id" : "pc-lab43",
								"ip" : "192.168.9.63",
								"username" : "test",
								"credential" : "test"
							]),
							([
								"id" : "pc-lab80",
								"ip" : "192.168.9.100",
								"username" : "test",
								"credential" : "test"
							])
						])
			])
}

