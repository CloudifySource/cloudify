												/*******************************************************************************
* Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
service {
	
	name "apacheLB"
	icon "feather-small.gif"
	type "WEB_SERVER"
	numInstances 1

	compute {
		template "SMALL_LINUX"
	}

	lifecycle {
	
	
		details {
			def currPublicIP
			
			if (  context.isLocalCloud()  ) {
				currPublicIP = InetAddress.localHost.hostAddress
			}
			else {
				currPublicIP =System.getenv()["CLOUDIFY_AGENT_ENV_PUBLIC_IP"]
			}
			def loadBalancerURL	= "http://${currPublicIP}:${currentPort}"
			def balancerManagerURL = "${loadBalancerURL}/balancer-manager"
			
			def ctxPath=("default" == context.applicationName)?"":"${context.applicationName}"
			
			def applicationURL = "${loadBalancerURL}/${ctxPath}"
		
				return [
					"BalancerManager URL":"<a href=\"${balancerManagerURL}\" target=\"_blank\">${balancerManagerURL}</a>",
					"Application URL":"<a href=\"${applicationURL}\" target=\"_blank\">${applicationURL}</a>"
				]
		}	
	
		install "apacheLB_install.groovy"
		postInstall "apacheLB_postInstall.groovy"
		start ([
			"Win.*":"run.bat",
			"Linux.*":"apacheLB_start.groovy"
			])
			
		startDetectionTimeoutSecs 800
		startDetection {			
			ServiceUtils.isPortOccupied(currentPort)
		}	
		
		preStop ([	
			"Win.*":"killAllHttpd.bat",		
			"Linux.*":"apacheLB_stop.groovy"
			])
		shutdown ([			
			"Linux.*":"apacheLB_uninstall.groovy"
		])
			
		locator {			
			def myPids = ServiceUtils.ProcessUtils.getPidsWithQuery("State.Name.re=httpd|apache")
			println ":apacheLB-service.groovy: current PIDs: ${myPids}"
			return myPids
        }
			
			
	}
	
	customCommands ([
		"addNode" : "apacheLB_addNode.groovy",
		"removeNode" : "apacheLB_removeNode.groovy",
		"load" : "apacheLB-load.groovy"		
	])
	
	
	network {
		port currentPort
		protocolDescription "HTTP"
	}
}
