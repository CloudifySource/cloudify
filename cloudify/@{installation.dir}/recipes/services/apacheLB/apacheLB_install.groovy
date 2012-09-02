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
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()
config = new ConfigSlurper().parse(new File("apacheLB-service.properties").toURL())

osConfig = ServiceUtils.isWindows() ? config.win32 : config.linux

downloadFile="${config.downloadFolder}/{$osConfig.zipName}"

def installLinuxHttpd(context,builder,currVendor,installScript) {

	if ( context.isLocalCloud() ) {
		if ( context.attributes.thisApplication["installing"] == null || context.attributes.thisApplication["installing"] == false ) {
			context.attributes.thisApplication["installing"] = true
		}
		else {
			while ( context.attributes.thisApplication["installing"] == true ) {
				println "apacheLB_install.groovy: Waiting for apt-get/yum (on localCloud) to end on another service instance in this application... "
				sleep 10000			
			}
		}
	}

	builder.sequential {
		echo(message:"apacheLB_install.groovy: Chmodding +x ${context.serviceDirectory} ...")
		chmod(dir:"${context.serviceDirectory}", perm:"+x", includes:"*.sh")

		echo(message:"apacheLB_install.groovy: Running ${context.serviceDirectory}/${installScript} os is ${currVendor}...")
		exec(executable: "${context.serviceDirectory}/${installScript}",failonerror: "true")
	}
	
	if ( context.isLocalCloud() ) {
		context.attributes.thisApplication["installing"] = false
		println "apacheLB_install.groovy: Finished using apt-get/yum on localCloud"
	}	
}

def installWindowsHttpd(config,osConfig,downloadFile,builder) {
	builder.sequential {
			echo(message:"apacheLB_install.groovy: installing on Windows...")
			mkdir(dir:"install")
			mkdir(dir:"${config.downloadFolder}")
			get(src:"${osConfig.downloadUrl}", dest:"${downloadFile}", skipexisting:true)
			unzip(src:"${downloadFile}", dest:"install", overwrite:true)
			copy( todir:'install' ) {
				fileset( dir:'overrides-win' )
			}	
	}
}

builder = new AntBuilder()

def os = OperatingSystem.getInstance()
def currVendor=os.getVendor()
switch (currVendor) {
		case ["Ubuntu", "Debian", "Mint"]:			
			installLinuxHttpd(context,builder,currVendor,"installOnUbuntu.sh")
			break		
		case ["Red Hat", "CentOS", "Fedora", "Amazon",""]:			
			installLinuxHttpd(context,builder,currVendor,"install.sh")
			break					
		case ~/.*(?i)(Microsoft|Windows).*/:		
			installWindowsHttpd(config,osConfig,downloadFile,builder)
			break
		default: throw new Exception("Support for ${currVendor} is not implemented")
}

