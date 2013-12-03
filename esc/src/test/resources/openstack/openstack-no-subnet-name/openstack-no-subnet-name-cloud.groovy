/***************
* Cloud configuration file for the Openstack cloud. *
*/
cloud {
        // Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
        name = "openstack"

        /********
         * General configuration information about the cloud driver implementation.
         */
        configuration {              
                className "org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver"
               
				networkDriverClassName "org.cloudifysource.esc.driver.provisioning.network.openstack.OpenstackNetworkDriver"
                             
                managementMachineTemplate "MEDIUM_LINUX"

                connectToPrivateIp true
        }

        /*************
         * Provider specific information.
         */
        provider {            
                provider "openstack-nova"
    
                machineNamePrefix "test-cloudify-agent-"
               
                managementOnlyFiles ([])
       
                sshLoggingLevel "WARNING"
          
                managementGroup "test-cloudify-manager-"
				
                numberOfManagementMachines 1

                reservedMemoryCapacityPerMachineInMB 1024

        }

        /*************
         * Cloud authentication information
         */
        user {
	            
	            user "${tenant}:${user}"  
	            apiKey apiKey

        }
        
        /********************
         * Cloud networking configuration.
         */
        cloudNetwork {

                // Details of the management network, which is shared among all instances of the Cloudify Cluster.
                management {
                        networkConfiguration {
                                // The network name
                                name "Cloudify-Management-Network"

                                // Subnets
                                subnets ([
                                        subnet {
                                                name "Cloudify-Management-Subnet"
                                                range "177.86.0.0/24"
                                                options ([ "gateway" : "177.86.0.111" ])
                                        }
                                ])
                                
                                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
                        }
                }
                    
                templates ([
                        "APPLICATION_NET" : networkConfiguration {
                                name "Cloudify-Application-Network"
                                subnets {                                     
                                        subnet {
                                               
                                                range "160.1.0.0/24"
                                                options ([ "gateway" : "null" ])
                                        }
                                }

                                custom ([ "associateFloatingIpOnBootstrap" : "true" ])
                        }
                ])       
        }
        
        cloudCompute {
                
                /***********
                 * Cloud machine templates available with this cloud.
                 */
                templates ([
                            MEDIUM_LINUX : computeTemplate{
                                  
                                    imageId imageId                                 
                                    remoteDirectory remoteDirectory                           
                                    machineMemoryMB 1600                                 
                                    hardwareId hardwareId                             
                                    localDirectory "upload"                                               
                                    fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP

                                    username "root"
									                                             
                                    options ([
                        						"computeServiceName" : "nova",
                                                "networkServiceName" : "quantum", // optional property (default: neutron)
                                                "keyPairName" : keyPair,
                                                  
                                             ])
                                    
                                    autoRestartAgent true
                                 
                                    overrides ([
                                            "jclouds.endpoint": openstackUrl
                                    ])
                            
                                    privileged true
                                  
                                    initializationCommand "#!/bin/sh\ncp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` > /etc/hosts\ncat  /tmp/hosts >> /etc/hosts"
                            }
                    ])
        
        }
}