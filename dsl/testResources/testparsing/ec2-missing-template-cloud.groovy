
cloud {
	name = "ec2"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver"
		managementMachineTemplate "MISSING_TEMPLATE"
		connectToPrivateIp true
	}

	provider {
		provider "aws-ec2"
		
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.0.1/gigaspaces-cloudify-2.0.1-m1-b1190-13.zip" 
		machineNamePrefix "NOA_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_cloudify_agent1"
		
		
		managementOnlyFiles ([])
		

		sshLoggingLevel "WARNING"
		managementGroup "NOA_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.MissingPemEc2Testsgtest_test.cli.cloudify.cloud.CleanGSFilesByonTestsgtest_cloudify_manager_1"
		numberOfManagementMachines 2
		
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "XXXXXXXXXXXXXXXXXX"
		apiKey "XXXXXXXXXXXXXXXXX"

	}
	
	cloudCompute {
		templates ([
			SMALL_LINUX : computeTemplate{
				//keyFile "cloud-demo.pem"
				localDirectory "upload"
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
}

