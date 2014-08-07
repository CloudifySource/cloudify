/*
 * ******************************************************************************
 *  * Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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
 * Cloud configuration file for the Softlayer cloud. 
 * Uses the default jClouds-based cloud driver but relies on Cloudify's extended 
 * jClouds implemetation for Softlayer.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 *
 * @author noak
 * @since 2.7.1
 */

cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = displayName

	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// Optional. The cloud implementation class. Defaults to the build in jclouds-based provisioning driver.
		className "org.cloudifysource.esc.driver.provisioning.jclouds.softlayer.SoftlayerProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "WINRMMGR"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		
        components {

            orchestrator {

                startMachineTimeoutInSeconds 36000
                stopMachineTimeoutInSeconds 36000
                minMemory "64m"
                maxMemory "1024m"
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
        // cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.7.1-6210-RELEASE/gigaspaces-cloudify-2.7.1-ga-b6210.zip"

		// Mandatory. The prefix for new machines started for servies.
        machineNamePrefix AGENT_PREFIX
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.


		//
		managementOnlyFiles ([])

		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "WARNING"

		// Mandatory. Name of the new machine/s started as cloudify management machines. Names are case-insensitive.
		managementGroup MANAGER_PREFIX
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
					WINRMMGR : computeTemplate {
						// Mandatory. Image ID.
						imageId winImageId
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						// using a short path
						remoteDirectory "/C\$/gs-files"
						
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 32768
						// Mandatory. Hardware ID.
						hardwareId largeHardwareId
						
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
						
						// Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
						// are not used.						
						locationId locationId
						username "administrator"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.CIFS
					
						// Remote execution mode. Options, defaults to SSH.
						remoteExecution org.cloudifysource.domain.cloud.RemoteExecutionModes.WINRM
						
						// Script language for remote execution. Defaults to Linux Shell.
						scriptLanguage org.cloudifysource.domain.cloud.ScriptLanguages.WINDOWS_BATCH
						
						installer {
							fileTransferConnectionTimeoutMillis 480000
							fileTransferConnectionRetryIntervalMillis 500000
							fileTransferRetries 200
							remoteExecutionConnectionTimeoutMillis 480000
							connectionTestIntervalMillis 2000
						}

                        options ([
                                "domainName":orgDomain,
								"networkVlanId":networkVlanId,
								"privateNetworkOnly": privateNetworkOnly,
								"startCpus":4,
								"maxMemory":32768,
								"operatingSystemReferenceCode":operatingSystemReferenceCode,
								"localDiskFlag":false,
								"blockDevicesDiskCapacity":[25],
								"maxNetworkSpeed":1000
								// optional. set HTTPS url to a post provisioning script
								// "postInstallScriptUri":postInstallScriptUri
                        ])
						
						overrides ([
							"jclouds.softlayer.package-id":packageId,
							"jclouds.so-timeout" : 600000,
							"jclouds.endpoint" : endpoint,
							"jclouds.connection-timeout" : 600000,
							"jclouds.request-timeout":600000,
							"jclouds.softlayer.virtualguest.active-transactions-ended-delay":9000000,
							"jclouds.max-retries":5
						])
						
						env ([
							"ESM_JAVA_OPTIONS" : "-Dorg.openspaces.grid.start-agent-timeout-seconds=10800000"
						])

						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true

                        // enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						// initializationCommand "echo Cloudify agent is about to start"
					},
					WINRMAGENT : computeTemplate {
						// Mandatory. Image ID.
						imageId winImageId
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						// using a short path
						remoteDirectory "/C\$/gs-files"
						
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 32768
						// Mandatory. Hardware ID.
						hardwareId largeHardwareId
						
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
						
						// Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
						// are not used.						
						locationId locationId
						username "administrator"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.CIFS
					
						// Remote execution mode. Options, defaults to SSH.
						remoteExecution org.cloudifysource.domain.cloud.RemoteExecutionModes.WINRM
						
						// Script language for remote execution. Defaults to Linux Shell.
						scriptLanguage org.cloudifysource.domain.cloud.ScriptLanguages.WINDOWS_BATCH
						
						installer {
							fileTransferConnectionTimeoutMillis 480000
							remoteExecutionConnectionTimeoutMillis 480000
							connectionTestIntervalMillis 2000
						}

                        options ([
                                "domainName":orgDomain,
								"networkVlanId":networkVlanId,
								"privateNetworkOnly": privateNetworkOnly,
								"startCpus":4,
								"maxMemory":32768,
								"operatingSystemReferenceCode":operatingSystemReferenceCode,
								"localDiskFlag":false,
								"blockDevicesDiskCapacity":[25],
								"maxNetworkSpeed":1000
								// optional. set HTTPS url to a post provisioning script
								// "postInstallScriptUri":postInstallScriptUri
                        ])
						
						overrides ([
							"jclouds.softlayer.package-id":packageId,
							"jclouds.so-timeout" : 600000,
							"jclouds.endpoint" : endpoint,
							"jclouds.connection-timeout" : 600000,
							"jclouds.request-timeout":600000,
							"jclouds.softlayer.virtualguest.active-transactions-ended-delay":9000000,
							"jclouds.max-retries":5
						])
						
						env ([
							"ESM_JAVA_OPTIONS" : "-Dorg.openspaces.grid.start-agent-timeout-seconds=10800000"
						])

						
						// when set to 'true', agent will automatically start after reboot.
						autoRestartAgent true

                        // enable sudo.
						privileged true
	
						// optional. A native command line to be executed before the cloudify agent is started.
						// initializationCommand "echo Cloudify agent is about to start"
					},
					WINRMAGENTFLEX : computeTemplate {
						// Mandatory. Image ID.
						imageId ""
						
						// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
						// using a short path
						remoteDirectory "/C\$/gs-files"
						
						// Mandatory. Amount of RAM available to machine.
						machineMemoryMB 32768
						// Mandatory. Hardware ID.
						hardwareId largeHardwareId
						
						// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
						localDirectory "upload"
						
						// Optional. Name of key file to use for authenticating to the remote machine. Remove this line if key files
						// are not used.						
						locationId locationId
						username "administrator"
						
						// File transfer mode.
						fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.CIFS
					
						// Remote execution mode. Options, defaults to SSH.
						remoteExecution org.cloudifysource.domain.cloud.RemoteExecutionModes.WINRM
						
						// Script language for remote execution. Defaults to Linux Shell.
						scriptLanguage org.cloudifysource.domain.cloud.ScriptLanguages.WINDOWS_BATCH
						
						installer {
							fileTransferConnectionTimeoutMillis 480000
							fileTransferConnectionRetryIntervalMillis 500000
							fileTransferRetries 200
							remoteExecutionConnectionTimeoutMillis 480000
							connectionTestIntervalMillis 2000
						}

                        options ([
                                "domainName":orgDomain,
								"networkVlanId":networkVlanId
                        ])
						
						overrides ([
							"jclouds.softlayer.package-id":packageId,
							"jclouds.so-timeout" : 600000,
							"jclouds.endpoint" : endpoint,
							"jclouds.connection-timeout" : 600000,
							"jclouds.request-timeout":600000,
							"jclouds.softlayer.virtualguest.active-transactions-ended-delay":9000000,
							"jclouds.max-retries":5,
							"jclouds.softlayer.flex-image-global-identifier" : "bd379ba4-c3fc-4d49-a24c-cf66e5938bb6"
						])
						
						env ([
							"ESM_JAVA_OPTIONS" : "-Dorg.openspaces.grid.start-agent-timeout-seconds=10800000"
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
		"org.cloudifysource.stop-management-timeout-in-minutes" : 30,
		"cleanupScriptOnMachineFailure" : "C:/gs-files/testcleanup.bat"
	])
}