import org.cloudifysource.dsl.context.ServiceContextFactory
config = new ConfigSlurper().parse(new File("mongod-service.properties").toURL())

def serviceContext = ServiceContextFactory.getServiceContext()



instanceID = serviceContext.getInstanceId()
println "mongod_start.groovy: mongod instanceID is ${instanceID}"

def home= serviceContext.attributes.thisInstance["home"]
println "mongod_start.groovy: mongod(${instanceID}) home ${home}"

def script= serviceContext.attributes.thisInstance["script"]
println "mongod_start.groovy: mongod(${instanceID}) script ${script}"

def port = serviceContext.attributes.thisInstance["port"] 

println "mongod_start.groovy: mongod(${instanceID}) port ${port}"

def dataDir = "${home}/data"
println "mongod_start.groovy: mongod(${instanceID}) dataDir ${dataDir}"

println "mongod_start.groovy: Running mongod(${instanceID}) script ${script} ..."

new AntBuilder().sequential {
	//creating the data directory 	
	mkdir(dir:"${dataDir}")
}

if (config.sharded) {
	new AntBuilder().sequential {
		exec(executable:"${script}") {
			arg line:"--journal"
			arg line:"--shardsvr"
			arg line:"--dbpath \"${dataDir}\""
			arg line:"--port ${port}"
		}
	}
} else {
	new AntBuilder().sequential {
		exec(executable:"${script}") {
			arg line:"--journal"
			arg line:"--dbpath \"${dataDir}\""
			arg line:"--port ${port}"
		}
	}
}




println "mongod_start.groovy: mongod(${instanceID}) script ${script} ended"



