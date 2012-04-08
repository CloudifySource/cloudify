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

config = new ConfigSlurper().parse(new File("tomcatScalingRules.properties").toURL())

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:config.installDir, overwrite:true)
}

if (config.applicationWarUrl && config.applicationWar) {
  new AntBuilder().sequential {	
    get(src:config.applicationWarUrl, dest:config.applicationWar, skipexisting:true)
    copy(todir: "${config.home}/webapps", file:config.applicationWar, overwrite:true)
  }
}

new AntBuilder().sequential {	
	chmod(dir:"${config.home}/bin", perm:'+x', includes:"*.sh")
}

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()
instanceID--
println "Replacing default tomcat port with port ${8080 + instanceID}"

serverXmlFile = new File("${config.home}/conf/server.xml") 
serverXmlText = serverXmlFile.text	
port8080ReplacementStr = "port=\"${8080 + instanceID}\""
port8009ReplacementStr = "port=\"${8009 + instanceID}\""
port8005ReplacementStr = "port=\"${8005 + instanceID}\""
serverXmlText = serverXmlText.replace("port=\"8080\"", port8080ReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8009\"", port8009ReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8005\"", port8005ReplacementStr) 
serverXmlFile.write(serverXmlText)

println "Replacing default jmxremote port with port ${11099 + instanceID}"
startScriptFile = new File("tomcatScalingRules_start.groovy")
startScriptText = startScriptFile.text
port11099ReplacementStr = "port=${11099 + instanceID}"
startScriptText = startScriptText.replaceAll("port=11099", port11099ReplacementStr)
startScriptFile.write(startScriptText)
