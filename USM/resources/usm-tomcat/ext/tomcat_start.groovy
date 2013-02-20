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

if ( "${config.dbServiceName}"!="NO_DB_REQUIRED" ) {
	println "tomcat_start.groovy: waiting for ${config.dbServiceName}..."
	def dbService = serviceContext.waitForService(config.dbServiceName, 20, TimeUnit.SECONDS) 
	def dbInstances = dbService.waitForInstances(dbService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
    dbServiceHost = dbInstances[0].hostAddress
	println "tomcat_start.groovy: ${config.dbServiceName} host is ${dbServiceHost}"
	def dbServiceInstances = serviceContext.attributes[config.dbServiceName].instances
	dbServicePort = dbServiceInstances[1].port
	println "tomcat_start.groovy: ${config.dbServiceName} port is ${dbServicePort}"
}
else {
	dbServiceHost="DUMMY_HOST"
	dbServicePort="DUMMY_PORT"
}	


println "tomcat_start.groovy executing ${script}"

portIncrement = 0
if (serviceContext.isLocalCloud()) {
  portIncrement = instanceID - 1  
}

currJmxPort=config.jmxPort+portIncrement
println "tomcat_start.groovy: jmx port is ${currJmxPort}"

new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${home}")
        env(key:"CATALINA_BASE", value: "${home}")
        env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=${currJmxPort} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
        env(key:"${config.dbHostVarName}", value: "${dbServiceHost}")
        env(key:"${config.dbPortVarName}", value: "${dbServicePort}")		
		arg(value:"run")
	}
	exec(executable:"${script}.bat", osfamily:"windows") { 
        env(key:"CATALINA_HOME", value: "${home}")
        env(key:"CATALINA_BASE", value: "${home}")
        env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=${currJmxPort} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
        env(key:"${config.dbHostVarName}", value: "${dbServiceHost}")
        env(key:"${config.dbPortVarName}", value: "${dbServicePort}")	
		arg(value:"run")
	}
}
