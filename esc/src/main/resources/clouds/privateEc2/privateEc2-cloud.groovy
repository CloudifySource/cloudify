/***************
 * Cloud configuration file for the Amazon ec2 cloud. Uses the default jclouds-based cloud driver.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 * @author barakme
 *
 */

cloud {
    // Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
    name = "privateEc2"

    /********
     * General configuration information about the cloud driver implementation.
     */
    configuration {
        // Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
        className "org.cloudifysource.esc.driver.provisioning.privateEc2.PrivateEC2CloudifyDriver"
        storageClassName "org.cloudifysource.esc.driver.provisioning.storage.aws.EbsStorageDriver"
        // Optional. The template name for the management machines. Defaults to the first template in the templates section below.
        managementMachineTemplate "CFN_MANAGER_TEMPLATE"   
    }

    /*************
     * Provider specific information.
     */
    provider {
        // Mandatory. The name of the provider.
        // When using the default cloud driver, maps to the Compute Service Context provider name.
        provider "aws-ec2"

        // Mandatory. The prefix for new machines started for servies.
        machineNamePrefix "cloudify-agent-"
        
        // Mandatory
        managementOnlyFiles ([])

        // Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
        sshLoggingLevel "WARNING"

        // Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
        managementGroup "cloudify-manager"
        // Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
        numberOfManagementMachines 1

        reservedMemoryCapacityPerMachineInMB 1024
    }

    /*************
     * Cloud authentication information
     */
    user {
        // Optional. Identity used to access cloud.
        // When used with the default driver, maps to the identity used to create the ComputeServiceContext.
        user accessKey

        // Optional. Key used to access cloud.
        // When used with the default driver, maps to the credential used to create the ComputeServiceContext.
        apiKey apiKey
    }
    
    cloudStorage {
        templates ([
            SMALL_BLOCK : storageTemplate{
                deleteOnExit true
                size 5
                path "/storage"
                namePrefix "cloudify-storage-volume"
                deviceName "/dev/sdc"
                fileSystemType "ext4"
                custom ([:])
            }
        ])
    }

    cloudCompute {
        
        /***********
         * Cloud machine templates available with this cloud.
         */
        templates ([
            // Mandatory. Template Name.
            CFN_TEMPLATE : computeTemplate{
                imageId ""             // For template validation, defined in CFN templates
                hardwareId ""          // For template validation, defined in CFN templates
                locationId locationId // For template validation
                machineMemoryMB 2048   // must be bigger than reservedMemoryCapacityPerMachineInMB+100                
                localDirectory "upload"
				remoteDirectory "/home/ubuntu/gs-files"
				custom ([
					"cfnTemplate":"privateEc2-cfn.template"
				])
            },
            CFN_MANAGER_TEMPLATE : computeTemplate{
                imageId ""             // For template validation, defined in CFN templates
                hardwareId ""          // For template validation, defined in CFN templates
                locationId locationId  // For template validation
                machineMemoryMB 2048   // must be bigger than reservedMemoryCapacityPerMachineInMB+100                
                localDirectory "upload"
				remoteDirectory "/home/ubuntu/gs-files"     // do not change, it is used to define $CLOUD_FILE
				
				custom ([
					"cfnManagerTemplate":"privateEc2-cfn.template",
					"s3BucketName":bucketName,
					"s3LocationId":bucketLocationId,
                    "debugMode":debugMode
				])
				
            },
        ])
    
    }
	custom ([
		// Optional. Path to an override file for the region metadata that maps service/region pairs to endpoints and vice versa.
		// If not set, the Amazon SDK will attempt to download an up-to-date set of region metadata from CloudFront
		// If a specific endpoint wasn't set, the endpoint will be selected according to location, using the region metadata.
		// "regionsFile":regionsFile,
		
		// Optional. Sets the requested protocol for accessing EC2 and S3. Defaults to HTTPS. The protocol must be supported by the 
		// relevant endpoint (as defined in the regions metadata). This setting can be overridden by specific endpoint settings, shown below.
		// "protocol":"HTTPS",

		// Optional. Specific endpoints for both the EC2 service.
		// If the URL contains a protocol (http/https) - it overrides the above protocol setting.
		// "endpoint":"http://ec2.us-east-1.amazonaws.com",
		
		// Optional. Specific endpoints for both the S3 service.
		// If the URL contains a protocol (http/https) - it overrides the above protocol setting.
		// "s3endpoint":"https://s3-eu-west-1.amazonaws.com"
		
	])
}