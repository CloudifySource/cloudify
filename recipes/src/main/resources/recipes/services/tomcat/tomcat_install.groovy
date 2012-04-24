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
import org.cloudifysource.dsl.context.ServiceContextFactory

config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()
installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID
home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:installDir)
	get(src:config.downloadPath, dest:"${installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${installDir}/${config.zipName}", dest:installDir, overwrite:true)
	move(file:"${installDir}/${config.unzipFolder}", tofile:"${home}")
}

if (config.applicationWarUrl && config.warName) {
  new AntBuilder().sequential {	
    get(src:config.applicationWarUrl, dest:"${installDir}/${config.warName}", skipexisting:true)
    copy(todir: "${home}/webapps", file:"${installDir}/${config.warName}", overwrite:true)
  }
}

new AntBuilder().sequential {	
	chmod(dir:"${home}/bin", perm:'+x', includes:"*.sh")
}

println "Replacing default tomcat port with port ${config.port}"
serverXmlFile = new File("${home}/conf/server.xml") 
serverXmlText = serverXmlFile.text	
portStr = "port=\"${config.port}\""
serverXmlFile.text = serverXmlText.replace('port="8080"', portStr) 