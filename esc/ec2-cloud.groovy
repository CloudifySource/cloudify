cloud {

	provider "aws-ec2"
	user ENTER_USER
	apiKey ENTER_KEY
	
	// relative path to gigaspaces directory
	localDirectory "tools/cli/plugins/esc/ec2/upload"
	
	remoteDirectory "/home/ec2-user/gs-files"
	
	imageId "us-east-1/ami-76f0061f"
	machineMemoryMB "1600"
	hardwareId "m1.small"

	securityGroup "default"
	
	keyFile ENTER_KEYFILE_PEM
	keyPair "cloud-demo"
	
//	cloudifyUrl "https://s3.amazonaws.com/test-repository-ec2dev/cloudify/gigaspaces.zip"
	machineNamePrefix "gs_esm_gsa_"

	dedicatedManagementMachines true
	managementOnlyFiles ([])
	connectedToPrivateIp false
	
	sshLoggingLevel java.util.logging.Level.WARNING
	managementGroup "management_machine"
	numberOfManagementMachines 2
	
	zones (["agent"])

	reservedMemoryCapacityPerMachineInMB 1024
	
	
}