/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

/***************
 * Cloud configuration file for the Amazon ec2 cloud. Uses the default jclouds-based cloud driver.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 * @author barakme
 *
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "softlayer"

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.jclouds.softlayer.SoftlayerProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true

        components {

            orchestrator {

                startMachineTimeoutInSeconds 36000
                stopMachineTimeoutInSeconds 36000
            }

        }


        // Optional. Path to folder where management state will be written. Null indicates state will not be written.
		persistentStoragePath persistencePath
		
		

	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		// When using the default cloud driver, maps to the Compute Service Context provider name.
		provider "softlayer"


		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// IMPORTANT: the default linux bootstrap script appends '.tar.gz' to the url whereas the default windows script appends '.zip'.
		// Therefore, if setting a custom URL, make sure to leave out the suffix.
        // cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.1-6203-M3/gigaspaces-cloudify-2.7.1-m3-b6203.zip"

		// Mandatory. The prefix for new machines started for servies.
        machineNamePrefix "cloudify-agent-"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.


		//
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
		user user

		// Optional. Key used to access cloud.
		// When used with the default driver, maps to the credential used to create the ComputeServiceContext.
		apiKey apiKey



	}
	
	cloudCompute {
		
		/***********
		 * Cloud machine templates available with this cloud.
		 */
		templates ([
					// Mandatory. Template Name.
					SMALL_LINUX : computeTemplate {
						// Mandatory. Image ID.
						imageId linuxImageId
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						remoteDirectory "/tmp/gs-files"
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 1600
						// Mandatory. Hardware ID.
						hardwareId hardwareId
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
						// Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
						// are not used.						
						locationId locationId
						username "root"

                        options ([
                                "domainName":"cloudify.org"
                        ])
						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true

                        // enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						// initializationCommand "echo Cloudify agent is about to start"
	
					},
                    // Mandatory. Template Name.
                    SMALL_LINUX_BMI : computeTemplate {
                        // Mandatory. Image ID.
                        imageId bmiLinuxImageId
                        // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                        remoteDirectory "/tmp/gs-files"
                        // Mandatory. Amount of RAM available to machine.
                        machineMemoryMB 1600
                        // Mandatory. Hardware ID.
                        hardwareId bmiHardwareId
                        // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                        localDirectory "upload"
                        // Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
                        // are not used.
                        locationId locationId
                        username "root"

                        // when set to 'true', agent will automatically start after reboot.
                        autoRestartAgent true

                        options ([
                                "domainName":"cloudify.org"
                        ])

                        overrides ([
                                "jclouds.softlayer.package-id":50 // Use Bare Metal Instances Package
                        ])

                        // enable sudo.
                        privileged true

                        // optional. A native command line to be executed before the cloudify agent is started.
                        // initializationCommand "echo Cloudify agent is about to start"
                    },
                    // Mandatory. Template Name.
                    MEDIUM_RH : computeTemplate {
                       // Mandatory. Image ID.
                       imageId readHatLinuxImageId
                       // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                       remoteDirectory "/tmp/gs-files"
                       // Mandatory. Amount of RAM available to machine.
                       machineMemoryMB 1600
                       // Mandatory. Hardware ID.
                       hardwareId readHatHardwareId
                       // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                       localDirectory "upload"
                       // Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
                       // are not used.						
                       locationId locationId
                       username "root"
					   
                       options ([
                              "domainName":"cloudify.org"
                       ])

                       // when set to 'true', agent will automatically start after reboot.
                       autoRestartAgent true

                       // enable sudo.
                      privileged true

                      // optional. A native command line to be executed before the cloudify agent is started.
                      // initializationCommand "echo Cloudify agent is about to start"
	
                   },
                   // Mandatory. Template Name.
                   MEDIUM_UBUNTU : computeTemplate {
                       // Mandatory. Image ID.
                       imageId mediumUbuntuLinuxImageId
                       // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                       remoteDirectory "/tmp/gs-files"
                       // Mandatory. Amount of RAM available to machine.
                       machineMemoryMB 1600
                       // Mandatory. Hardware ID.
                       hardwareId mediumUbuntuHardwareId
                       // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                       localDirectory "upload"
                       // Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
                       // are not used.						
                       locationId locationId
                       username "root"

                       options ([
                              "domainName":"cloudify.org"
                       ])

                       // when set to 'true', agent will automatically start after reboot.
                       autoRestartAgent true

                       // enable sudo.
                       privileged true

                       // optional. A native command line to be executed before the cloudify agent is started.
                       // initializationCommand "echo Cloudify agent is about to start"

                   },
                   // Mandatory. Template Name.
                   BLU_FOR_CLOUD : computeTemplate {
                       // Mandatory. Files from the local directory will be copied to this directory on the remote machine.
                       remoteDirectory "/tmp/gs-files"
                       // Mandatory. Amount of RAM available to machine.
                       machineMemoryMB 1600
                       // Mandatory. Hardware ID.
                       hardwareId hardwareId
                       // Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
                       localDirectory "upload"
                       // Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
                       // are not used.
                       locationId locationId
                       username "root"

                       options ([
                               "domainName":"cloudify.org"
                       ])

                       overrides ([
                               "jclouds.softlayer.package-id":46,
                               "jclouds.softlayer.flex-image-global-identifier":"5ad7a379-5252-4c19-a02a-1a5882f4fcbf"
                       ])

                       // when set to 'true', agent will automatically start after reboot.
                       autoRestartAgent true

                       // enable sudo.
                       privileged true

                       // optional. A native command line to be executed before the cloudify agent is started.
                       // initializationCommand "echo Cloudify agent is about to start"
                   }
        ])
	
	}

	custom ([			
		"org.cloudifysource.stop-management-timeout-in-minutes" : 30
	])
}