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

config=new ConfigSlurper().parse(new File("tomcat.properties").toURL())

println "tomcat_start.groovy: Calculating mongoServiceHost..."
serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()
println "tomcat_start.groovy: This tomcat instance ID is ${instanceID}"

println "tomcat_start.groovy: waiting for ${config.mongoService}..."
mongoService = serviceContext.waitForService(config.mongoService, 20, TimeUnit.SECONDS) 
mongoInstances = mongoService.waitForInstances(mongoService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
def mongoServiceHost = mongoInstances[instanceID-1].hostAddress
println "tomcat_start.groovy: Mongo service host is ${mongoServiceHost}"

def mongoServiceInstances=serviceContext.attributes.mongos.instances
mongoServicePort=mongoServiceInstances[instanceID].port


println "tomcat_start.groovy executing ${config.script}"
new AntBuilder().sequential {
	exec(executable:"${config.script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoServiceHost}")
        env(key:"MONGO_PORT", value: "${mongoServicePort}")
		arg(value:"run")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows") { 
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoServiceHost}")
        env(key:"MONGO_PORT", value: "${mongoServicePort}")
		arg(value:"run")
	}
}
