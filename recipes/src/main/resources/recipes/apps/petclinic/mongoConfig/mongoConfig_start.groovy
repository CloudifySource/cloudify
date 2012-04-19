import org.cloudifysource.dsl.context.ServiceContextFactory

def config = new ConfigSlurper().parse(new File('mongoConfig.properties').toURL())

def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()

def intPort = serviceContext.attributes.thisInstance["port"] as int
println "mongoConfig_start.groovy: mongoConfig#${instanceID} is using port ${intPort}"

def home = serviceContext.attributes.thisInstance["home"]
println "mongoConfig_start.groovy: home ${home}"

def script= serviceContext.attributes.thisInstance["script"]
println "mongoConfig_start.groovy: script ${script}"


def dataDir = "${home}/data/cfg"
println "mongoConfig_start.groovy: dataDir is ${dataDir}"

println "mongoConfig_start.groovy: Running script ${script} for mongoConfig#${instanceID}..."
new AntBuilder().sequential {
	//creating the data directory 	
    
	mkdir(dir:dataDir)
	exec(executable:"${script}") {
		arg line:"--journal"
		arg value:"--configsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${intPort}"
    }
}

println "mongoConfig_start.groovy: Script ${script} ended for mongoConfig#${instanceID}"
