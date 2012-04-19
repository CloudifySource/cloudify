@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo
import org.cloudifysource.dsl.context.ServiceContextFactory

config = new ConfigSlurper().parse(new File("mongod-service.properties").toURL())

serviceContext = ServiceContextFactory.getServiceContext()


instanceID = serviceContext.getInstanceId()

println "mongod_poststart.groovy: instanceID ${instanceID}"
port= serviceContext.attributes.thisInstance["port"]
println "mongod_poststart.groovy: in port obj is ${port}"




println "mongod_poststart.groovy: Sleeping for 5 secs and then check port ${port}"
sleep(5000)

println "mongod_poststart.groovy: Checking connection to mongo on port ${port}"
try {
    //check connection 
	mongo = new GMongo("127.0.0.1", port)
	db = mongo.getDB("petclinic")
	assert db != null 
    println "mongod_poststart.groovy: Connection succeeded"
	mongo.close()
} 
catch (Exception e) {
    println "mongod_poststart.groovy: Connection Failed!"
	throw e; 
}

