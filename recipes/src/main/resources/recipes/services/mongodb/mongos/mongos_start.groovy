import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos-service.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
mongoCfgService = serviceContext.waitForService("mongoConfig", 20, TimeUnit.SECONDS)
mongoCfgInstances = mongoCfgService.waitForInstances(mongoCfgService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
instanceID = serviceContext.getInstanceId()
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${instanceID}"
mongoCfgHost = mongoCfgInstances[instanceID-1].hostAddress
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${mongoCfgHost}"


currPort = serviceContext.attributes.thisInstance["port"]
println "mongos_start.groovy: mongos(${instanceID}) port ${currPort}"


cfgPort=serviceContext.attributes."mongoConfig".instances[instanceID].port
println "mongos_start.groovy: mongoConfig(${instanceID}) port is ${cfgPort}"

script= serviceContext.attributes.thisInstance["script"]
println "mongos_start.groovy: script ${script}"


println "mongos_start.groovy: Running script ${script} for mongos#${instanceID} ..."
new AntBuilder().exec(executable:"${script}") {
	arg line:"--configdb ${mongoCfgHost}:${cfgPort}"
	arg line:"--port ${currPort}"
	arg line:"--chunkSize 1"
}

println "mongos_start.groovy: Script ${script} ended for mongos#${instanceID}"


