cloud {
	
	user "USER"
	apiKey "KEY"
	provider "EC2"
	localDirectory "local"
	remoteDirectory "remote"
	
	imageId "ami"
	machineMemoryMB "2048"
	hardwareId "m1.small"

	
	cloudifyUrl "http://someplace"
	machineNamePrefix "agent_"

	dedicatedManagementMachines true
	//managementOnlyFiles ""
	connectedToPrivateIp false

}