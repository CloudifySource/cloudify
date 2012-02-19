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

config = new ConfigSlurper().parse(new File('mongoConfig.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()

def port = serviceContext.attributes.thisInstance["port"] 
intPort=port.intValue()

println "mongoConfig_start.groovy: mongoConfig#${instanceID} is using port ${intPort}"


dataDir = "${config.home}/data/cfg"
println "mongoConfig_start.groovy: dataDir is ${dataDir}"

println "mongoConfig_start.groovy: Running script ${config.script} for mongoConfig#${instanceID}..."
new AntBuilder().sequential {
	//creating the data directory 	
	mkdir(dir:dataDir)
	exec(executable:"${config.script}") {
		arg value:"--configsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${intPort}"
    }
}

println "mongoConfig_start.groovy: Script ${config.script} ended for mongoConfig#${instanceID}"
