cloud2 {
	
	
	provider {
		provider "aws-ec2"
		localDirectory "tools/cli/plugins/esc/ec2/upload"
		
		cloudifyUrl "https://s3.amazonaws.com/test-repository-ec2dev/cloudify/gigaspaces.zip"
		machineNamePrefix "gs_esm_gsa_"
		securityGroup "default"

		dedicatedManagementMachines true
		managementOnlyFiles ([])
		connectedToPrivateIp false

		sshLoggingLevel java.util.logging.Level.WARNING
		managementGroup "management_machine"
		numberOfManagementMachines 2
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
	}
	user {
		user "0VCFNJS3FXHYC7M6Y782"
		apiKey "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO"
		keyFile "cloud-demo.pem"
		keyPair "cloud-demo"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "us-east-1/ami-76f0061f"
					machineMemoryMB "1600"
					hardwareId "m1.small"
					remoteDirectory "/home/ec2-user/gs-files"
				}
			])
}
