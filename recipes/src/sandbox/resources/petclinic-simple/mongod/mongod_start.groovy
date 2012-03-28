import org.cloudifysource.dsl.context.ServiceContextFactory


def serviceContext = ServiceContextFactory.getServiceContext()



instanceID = serviceContext.getInstanceId()
println "mongod_start.groovy: mongod instanceID is ${instanceID}"

def home= serviceContext.attributes.thisInstance["home"]
println "mongod_start.groovy: mongod(${instanceID}) home ${home}"

def script= serviceContext.attributes.thisInstance["script"]
println "mongod_start.groovy: mongod(${instanceID}) script ${script}"

def port = serviceContext.attributes.thisInstance["port"] 
intPort=port.intValue()

println "mongod_start.groovy: mongod(${instanceID}) port ${intPort}"

def dataDir = "${home}/data"
println "mongod_start.groovy: mongod(${instanceID}) dataDir ${dataDir}"

println "mongod_start.groovy: Running mongod(${instanceID}) script ${script} ..."

new AntBuilder().sequential {
	//creating the data directory 	
	mkdir(dir:"${dataDir}")
    
	exec(executable:"${script}") {
		arg line:"--journal"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${intPort}"
	}
}

println "mongod_start.groovy: mongod(${instanceID}) script ${script} ended"



