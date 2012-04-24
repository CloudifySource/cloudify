import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos-service.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
mongoCfgService = serviceContext.waitForService("mongoConfig", 20, TimeUnit.SECONDS)
mongoCfgInstances = mongoCfgService.waitForInstances(mongoCfgService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
instanceID = serviceContext.getInstanceId()

mongoConfigPort=serviceContext.attributes."mongoConfig".instances[instanceID].port
println "mongos_start.groovy: mongoConfig(${instanceID}) port is ${mongoConfigPort}"

if (mongoCfgInstances.length ==3) {
configdb = "${mongoCfgInstances[0].hostAddress}:${mongoConfigPort},${mongoCfgInstances[1].hostAddress}:${mongoConfigPort},${mongoCfgInstances[2].hostAddress}:${mongoConfigPort}"
}
else if (mongoCfgInstances.length ==1) {
configdb = "${mongoCfgInstances[0].hostAddress}:${mongoConfigPort}"
}
else {
  throw new IllegalStateException("mongoConfig should have 1 or 3 instances");
}

println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${configdb}"


currPort = serviceContext.attributes.thisInstance["port"]
println "mongos_start.groovy: mongos(${instanceID}) port ${currPort}"



script= serviceContext.attributes.thisInstance["script"]
println "mongos_start.groovy: script ${script}"


println "mongos_start.groovy: Running script ${script} for mongos#${instanceID} ..."
new AntBuilder().exec(executable:"${script}") {
	arg line:"--configdb ${configdb}"
	arg line:"--port ${currPort}"
	arg line:"--chunkSize 1"
}

println "mongos_start.groovy: Script ${script} ended for mongos#${instanceID}"


