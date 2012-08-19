cloud2 {
	
	
	provider {
		provider "aws-ec2"
		localDirectory "tools/cli/plugins/esc/ec2/upload"
		
		cloudifyUrl "https://s3.amazonaws.com/test-repository-ec2dev/cloudify/gigaspaces.zip"
		machineNamePrefix "gs_esm_gsa_"
		securityGroup "default"

		
		managementOnlyFiles ([])
		connectedToPrivateIp false

		sshLoggingLevel java.util.logging.Level.WARNING
		managementGroup "management_machine"
		numberOfManagementMachines 2
		
		reservedMemoryCapacityPerMachineInMB 1024
	}
	user {
		user "XXXXXXXXXXXXXXXXXXX"
		apiKey "XXXXXXXXXXXXXXXXXXXXXXXXX"
		
		keyPair "cloud-demo"
	}
	templates ([
				SMALL_LINUX : template{
					keyFile "cloud-demo.pem"
					imageId "us-east-1/ami-76f0061f"
					machineMemoryMB "1600"
					hardwareId "m1.small"
					remoteDirectory "/home/ec2-user/gs-files"
				}
			])
}
