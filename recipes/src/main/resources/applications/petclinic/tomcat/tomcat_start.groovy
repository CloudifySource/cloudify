import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config=new ConfigSlurper().parse(new File("tomcat.properties").toURL())

println "#################### calculating mongoHost"
println "waiting for ${config.mongoService}"
serviceContext = ServiceContextFactory.getServiceContext()
mongoService = serviceContext.waitForService(config.mongoService, 20, TimeUnit.SECONDS) 
mongoInstances = mongoService.waitForInstances(mongoService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
def mongoHost = mongoInstances[0].hostAddress
println "#################### got mongo host ${mongoHost}"

//start tomcat
println "executing command ${config.script}"
new AntBuilder().sequential {
	exec(executable:"${config.script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoHost}")
        env(key:"MONGO_PORT", value: "${config.mongoPort}")
		arg(value:"run")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows") { 
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"MONGO_HOST", value: "${mongoHost}")
        env(key:"MONGO_PORT", value: "${config.mongoPort}")
		arg(value:"run")
	}
}
