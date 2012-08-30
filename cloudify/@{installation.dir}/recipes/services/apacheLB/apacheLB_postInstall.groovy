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

def ctxPath = ("default" == context.applicationName)?"":"${context.applicationName}"

builder = new AntBuilder()


def os = OperatingSystem.getInstance()
def currVendor=os.getVendor()
def confScript
def origProxyBalancerPath
def proxyBalancerName
def isLinux
switch (currVendor) {
	case ["Ubuntu", "Debian", "Mint"]:			
		confScript="${context.serviceDirectory}/configureApacheConfOnUbuntu.sh"
		origProxyBalancerPath="${context.serviceDirectory}/overrides-linux/conf/ubuntu"
		proxyBalancerName="proxy_balancer.conf"
		isLinux=true
		break		
	case ["Red Hat", "CentOS", "Fedora", "Amazon",""]:			
		confScript="${context.serviceDirectory}/configureApacheConf.sh"
		origProxyBalancerPath="${context.serviceDirectory}/overrides-linux/conf/extra"
		proxyBalancerName="httpd-proxy-balancer.conf"
		isLinux=true
		break					
	case ~/.*(?i)(Microsoft|Windows).*/:		
		confScript=""
		isLinux=false
		proxyBalancerName="httpd-proxy-balancer.conf"
		break
	default: throw new Exception("Support for ${currVendor} is not implemented")
}


if ( isLinux ) {
	builder.sequential {		
		echo(message:"apacheLB_install.groovy: Running ${confScript} os is ${currVendor}...")
		exec(executable: "${confScript}",failonerror: "true") {
			arg(value:"80")		
			arg(value:"${config.currentPort}")
			arg(value:"${origProxyBalancerPath}")	
			arg(value:"${ctxPath}")	
			arg(value:"${config.useStickysession}")
			arg(value:"${context.serviceDirectory}")
		}	
	}
}


def proxyConfigFile

def proxyConfigText=""

if( !isLinux ) {
	println "apacheLB_postInstall.groovy: Replacing STICKYSESSION_PLACE_HOLDER in ${proxyBalancerName}..."
	context.attributes.thisInstance["proxyBalancerPath"] = "${context.serviceDirectory}/install/conf/extra/${proxyBalancerName}"
	proxyConfigFile = new File("${context.serviceDirectory}/install/conf/extra/${proxyBalancerName}")
	proxyConfigText = proxyConfigFile.text	
	proxyConfigText = proxyConfigText.replace("PATH-TO-APP", "${ctxPath}")
	if ( "${config.useStickysession}"=="true" ) {
		println "apacheLB_postInstall.groovy: Using Stickysession ..."
		proxyConfigText = proxyConfigText.replace("STICKYSESSION_PLACE_HOLDER","stickysession=JSESSIONID|jsessionid nofailover=Off")
	}
	else {	
		println "apacheLB_postInstall.groovy: Not USING Stickysession ..."
		proxyConfigText = proxyConfigText.replace("STICKYSESSION_PLACE_HOLDER","")
	}
	
	confFile = new File("install/conf/httpd.conf")
	confFileText = confFile.text
	println "apacheLB_postInstall.groovy: Replacing 8090 with ${config.currentPort} in httpd.conf..."
	confFileText = confFileText.replace("8090", "${config.currentPort}")
	confFile.text = confFileText
}
else {
	println "apacheLB_postInstall.groovy: Looking for the path of ${proxyBalancerName} in ${context.serviceDirectory}/proxyBalancerPath ..."
	proxyBalancerFile = new ConfigSlurper().parse(new File("${context.serviceDirectory}/proxyBalancerPath").toURL())	                    
	def proxyBalancerFullPath="${proxyBalancerFile.pathToBalancerConf}"
	println "apacheLB_postInstall.groovy: conf file is in ${proxyBalancerFullPath}"
	context.attributes.thisInstance["proxyBalancerPath"] = "${proxyBalancerFullPath}"	
	proxyConfigFile = new File("${proxyBalancerFullPath}")	
	proxyConfigText = proxyConfigFile.text	
}


/* Now look for the WL instances (if registerd before) and register their IPs now */
def balancerMembers = context.attributes.thisService["balancerMembers"]

if ( balancerMembers!=null ) {
		
	/* This means that this LB service failed in the past and now it's being re-installed.
	   So, the members need to be re-added to the LB.
		
	   Here, we remove the 1st and last comma from the node list */
	balancerMembers = balancerMembers.substring(1,balancerMembers.length()-1)
	def dontModify="# Generated code - DO NOT MODIFY"
	balancerMembers.split(",,").each { currNode ->
		if ( proxyConfigText.contains(currNode)) {
			println "apacheLB_postInstall.groovy: Not adding ${currNode} to httpd-proxy-balancer.conf because it's already there..."	
		}
		else {
			println "apacheLB_postInstall.groovy: Adding ${currNode} to ${proxyBalancerName} .. "
			proxyConfigText = proxyConfigText.replace("${dontModify}", "${dontModify}" + System.getProperty("line.separator") + "${currNode}")
		}
	}	
}

proxyConfigFile.text = proxyConfigText





