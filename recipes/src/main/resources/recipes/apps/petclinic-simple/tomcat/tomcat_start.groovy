import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

def config=new ConfigSlurper().parse(new File("tomcat-service.properties").toURL())

println "tomcat_start.groovy: Calculating mongoServiceHost..."
def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()
println "tomcat_start.groovy: This tomcat instance ID is ${instanceID}"

def home= serviceContext.attributes.thisInstance["home"]
println "tomcat_start.groovy: tomcat(${instanceID}) home ${home}"

def script= serviceContext.attributes.thisInstance["script"]
println "tomcat_start.groovy: tomcat(${instanceID}) script ${script}"



println "tomcat_start.groovy: waiting for ${config.mongoService}..."
def mongoService = serviceContext.waitForService(config.mongoService, 20, TimeUnit.SECONDS) 
def mongoInstances = mongoService.waitForInstances(mongoService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
def mongoServiceHost = mongoInstances[instanceID-1].hostAddress
println "tomcat_start.groovy: Mongo service host is ${mongoServiceHost}"

def mongoServiceInstances = serviceContext.attributes[config.mongoService].instances
def mongoServicePort = mongoServiceInstances[instanceID].port


println "tomcat_start.groovy executing ${script}"

portIncrement = 0
if (serviceContext.isLocalCloud()) {
  portIncrement = instanceID - 1  
}

currJmxPort=config.jmxPort+portIncrement
println "tomcat_start.groovy: Replacing default jmx port with port ${currJmxPort}"

new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=${currJmxPort} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoServiceHost}")
        env(key:"MONGO_PORT", value: "${mongoServicePort}")
		arg(value:"run")
	}
	exec(executable:"${script}.bat", osfamily:"windows") { 
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=${currJmxPort} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoServiceHost}")
        env(key:"MONGO_PORT", value: "${mongoServicePort}")
		arg(value:"run")
	}
}
