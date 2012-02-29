import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

def config = new ConfigSlurper().parse(new File('mongos.properties').toURL())

def serviceContext = ServiceContextFactory.getServiceContext()
def mongoCfgService = serviceContext.waitForService("mongoConfig", 20, TimeUnit.SECONDS)
def mongoCfgInstances = mongoCfgService.waitForInstances(mongoCfgService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
def instanceID = serviceContext.getInstanceId()
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${instanceID}"
def mongoCfgHost = mongoCfgInstances[instanceID-1].hostAddress
println "mongos_start.groovy: mongos#${instanceID} is using mongoConfig ${mongoCfgHost}"


def intPort = serviceContext.attributes.thisInstance["port"] as int 
println "mongos_start.groovy: mongos(${instanceID}) port ${intPort}"


def cfgPort=serviceContext.attributes."mongoConfig".instances[instanceID].port
println "mongos_start.groovy: mongoConfig(${instanceID}) port is ${cfgPort}"

def script= serviceContext.attributes.thisInstance["script"]
println "mongos_start.groovy: script ${script}"


println "mongos_start.groovy: Running script ${script} for mongos#${instanceID} ..."
new AntBuilder().exec(executable:"${script}") {
	arg line:"--configdb ${mongoCfgHost}:${cfgPort}"
	arg line:"--port ${intPort}"
	arg line:"--chunkSize 1"
}

println "mongos_start.groovy: Script ${script} ended for mongos#${instanceID}"


