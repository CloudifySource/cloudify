import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
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
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"CASSANDRA_IP", value:cassandraHost)
		arg(value:"run")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows") {
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		env(key:"CASSANDRA_IP", value:cassandraHost)
		arg(value:"run")
	}
}
