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
import org.hyperic.sigar.OperatingSystem
import org.cloudifysource.dsl.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()
config = new ConfigSlurper().parse(new File("apacheLB-service.properties").toURL())


def node = args[0]
def instanceID= args[1]

def proxyBalancerFullPath = context.attributes.thisInstance["proxyBalancerPath"]

println "addNode: About to add ${node} instance (${instanceID}) to httpd-proxy-balancer.conf..."
def proxyConfigFile = new File("${proxyBalancerFullPath}")
def configText = proxyConfigFile.text
def routeStr=""
if ( "${config.useStickysession}" == "true" ) {
	routeStr=" route=" + instanceID
}

def balancerMemberText="BalancerMember " + node + routeStr

if ( configText.contains(balancerMemberText)) {
	println "addNode: Not adding ${node} to httpd-proxy-balancer.conf because it's already there..."	
}
else { 
	def modifiedConfig = configText.replace("# Generated code - DO NOT MODIFY", "# Generated code - DO NOT MODIFY" + System.getProperty("line.separator") + "${balancerMemberText}")				
	proxyConfigFile.text = modifiedConfig
	println "addNode: Added ${node} to httpd-proxy-balancer.conf text is now : ${modifiedConfig}..."

	def balancerMembers=context.attributes.thisService["balancerMembers"]
	if ( balancerMembers == null ) {
		balancerMembers = ""
	}
	balancerMembers +=",${balancerMemberText},"										
	context.attributes.thisService["balancerMembers"]=balancerMembers
	println "addNode: Added ${node} to context balancerMembers which is now ${balancerMembers}"


	currOs=System.properties['os.name']
	println "addNode: About to kill ${currOs} processes ..."
	if ("${currOs}".toLowerCase().contains('windows')) {
		println "addNode: About to kill httpd.."
		def currCmd="taskkill /t /im httpd* /f"
		currCmd.execute()
		println "addNode: Killed httpd"
	}
	else {
		def os = OperatingSystem.getInstance()
		def currVendor=os.getVendor()
		def stopScript
		switch (currVendor) {
			case ["Ubuntu", "Debian", "Mint"]:			
				stopScript="${context.serviceDirectory}/stopOnUbuntu.sh"
				break		
			case ["Red Hat", "CentOS", "Fedora", "Amazon",""]:			
				stopScript="${context.serviceDirectory}/stop.sh"
				break					
			default: throw new Exception("Support for ${currVendor} is not implemented")
		}

		builder = new AntBuilder()
		builder.sequential {
			exec(executable:"${stopScript}", osfamily:"unix")        	
		}
	}
}

