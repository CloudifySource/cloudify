import org.cloudifysource.dsl.context.ServiceContextFactory

config = new ConfigSlurper().parse(new File('mongoConfig-service.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()

currPort = serviceContext.attributes.thisInstance["port"]
println "mongoConfig_start.groovy: mongoConfig#${instanceID} is using port ${currPort}"

home = serviceContext.attributes.thisInstance["home"]
println "mongoConfig_start.groovy: home ${home}"

script= serviceContext.attributes.thisInstance["script"]
println "mongoConfig_start.groovy: script ${script}"


dataDir = "${home}/data/cfg"
println "mongoConfig_start.groovy: dataDir is ${dataDir}"

println "mongoConfig_start.groovy: Running script ${script} for mongoConfig#${instanceID}..."
new AntBuilder().sequential {
	//creating the data directory 	
    
	mkdir(dir:dataDir)
	exec(executable:"${script}") {
		arg line:"--journal"
		arg value:"--configsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${currPort}"
    }
}

println "mongoConfig_start.groovy: Script ${script} ended for mongoConfig#${instanceID}"
