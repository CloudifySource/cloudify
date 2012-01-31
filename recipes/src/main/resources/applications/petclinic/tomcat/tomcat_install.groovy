/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

println "tomcat_install.groovy: Installing tomcat..."

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:"${config.installDir}")
	get(src:"${config.downloadPath}", dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:"${config.installDir}", overwrite:true)
	get(src:"${config.applicationWarUrl}", dest:"${config.applicationWar}", skipexisting:true)
	copy(todir: "${config.home}/webapps", file:"${config.applicationWar}", overwrite:true)
	chmod(dir:"${config.home}/bin", perm:'+x', includes:"*.sh")
}

println "tomcat_install.groovy: Replacing default tomcat port with port ${config.port}"
serverXmlFile = new File("${config.home}/conf/server.xml") 
serverXmlText = serverXmlFile.text	
portStr = "port=\"${config.port}\""
serverXmlFile.text = serverXmlText.replace('port="8080"', portStr) 

println "tomcat_install.groovy: Tomcat installation ended"