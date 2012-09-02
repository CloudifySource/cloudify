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

println "removeNode: About to remove ${node} instance (${instanceID}) to httpd-proxy-balancer.conf..."
def proxyConfigFile = new File("${proxyBalancerFullPath}")
			
def routeStr=""
if ( "${config.useStickysession}" == "true" ) {
	routeStr=" route=" + instanceID
}
	
def balancerMemberText="BalancerMember " + node + routeStr

def configText = proxyConfigFile.text
def modifiedConfig = configText.replace("${balancerMemberText}", "")
proxyConfigFile.text = modifiedConfig
println "removeNode: Removed ${node} from httpd-proxy-balancer.conf text is now : ${modifiedConfig}..."

def balancerMembers=context.attributes.thisService["balancerMembers"]
balancerMembers=balancerMembers.replace(",${balancerMemberText},","")
if ( balancerMembers == "" ) {
	balancerMembers = null
}
context.attributes.thisService["balancerMembers"]=balancerMembers
println "removeNode: Cleaned ${node} from context balancerMembers"

currOs=System.properties['os.name']
println "removeNode: About to kill ${currOs} processes ..."
if ("${currOs}".toLowerCase().contains('windows')) {
	def currCmd="taskkill /t /im httpd* /f"
	println "removeNode: About to kill httpd.."
	currCmd.execute()
	println "removeNode: Killed httpd"
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
		