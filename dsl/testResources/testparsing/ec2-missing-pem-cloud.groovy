
cloud {
	name = "ec2"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp true
	}

	provider {
		provider "aws-ec2"
		localDirectory "tools/cli/plugins/esc/ec2/upload"
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.0.1/gigaspaces-cloudify-2.0.1-m1-b1190-13.zip" 
		machineNamePrefix "NOA_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_cloudify_agent1"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		

		sshLoggingLevel "WARNING"
		managementGroup "NOA_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.CleanGSFilesByonTestsgtest_cloudify_manager_1"
		numberOfManagementMachines 2
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "0VCFNJS3FXHYC7M6Y782"
		apiKey "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO"
		keyFile "cloud-demo1.pem"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "us-east-1/ami-76f0061f"
					machineMemoryMB 1600
					remoteDirectory "/home/ec2-user/gs-files"
					hardwareId "m1.small"
					locationId "us-east-1"
					options ([
						"securityGroups" : ["default"] as String[],
						"keyPair" : "cloud-demo"
					])
				}
			])
}

