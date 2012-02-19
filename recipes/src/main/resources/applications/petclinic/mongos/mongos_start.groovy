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
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
mongoCfgService = serviceContext.waitForService("mongoConfig", 20, TimeUnit.SECONDS)
mongoCfgInstances = mongoCfgService.waitForInstances(mongoCfgService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
instanceID = serviceContext.getInstanceId()
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${instanceID}"
mongoCfgHost = mongoCfgInstances[instanceID-1].hostAddress
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${mongoCfgHost}"


def port = serviceContext.attributes.thisInstance["port"] 
intPort=port.intValue()
println "mongos_start.groovy: mongos(${instanceID}) port ${intPort}"


cfgPort=serviceContext.attributes."mongoConfig".instances[instanceID].port
println "mongos_start.groovy: mongoConfig(${instanceID}) port is ${cfgPort}"

println "mongos_start.groovy: Running script ${config.script} for mongos#${instanceID} ..."
new AntBuilder().exec(executable:"${config.script}") {
	arg line:"--configdb ${mongoCfgHost}:${cfgPort}"
	arg line:"--port ${intPort}"
	arg line:"--chunkSize 1"
}

println "mongos_start.groovy: Script ${config.script} ended for mongos#${instanceID}"


