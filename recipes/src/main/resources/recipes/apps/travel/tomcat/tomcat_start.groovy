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
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

println "#################### calculating cassandra host"
println "waiting for cassandra"
serviceContext = ServiceContextFactory.getServiceContext()
cassandraService = serviceContext.waitForService("cassandra", 20, TimeUnit.SECONDS) 
cassandraInstances = cassandraService.waitForInstances(cassandraService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
cassandraHost = cassandraInstances[0].hostAddress

println "#################### got cassandra host: ${cassandraHost}"

//start tomcat
println "executing command ${config.script}"
new AntBuilder().sequential {
	exec(executable:"${config.script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${config.home}")
    env(key:"CATALINA_BASE", value: "${config.home}")
    env(key:"CATALINA_TMPDIR", value: "${config.home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"CASSANDRA_IP", value:cassandraHost)
		arg(value:"run")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows") {
        env(key:"CATALINA_HOME", value: "${config.home}")
    env(key:"CATALINA_BASE", value: "${config.home}")
    env(key:"CATALINA_TMPDIR", value: "${config.home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"CASSANDRA_IP", value:cassandraHost)
		arg(value:"run")
	}
}
