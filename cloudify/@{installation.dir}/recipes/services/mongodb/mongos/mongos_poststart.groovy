@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo

import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos-service.properties').toURL())

println "mongos_poststart.groovy: sleeping for 60 secs..."
sleep(60)

serviceContext = ServiceContextFactory.getServiceContext()

println "mongos_poststart.groovy: serviceContext object is " + serviceContext

//waiting for mongod service to become available, will not be needed in one of the upcoming builds 
println "mongos_poststart.groovy: Waiting for mongod..."
mongodService = serviceContext.waitForService("mongod", 20, TimeUnit.SECONDS)
if (mongodService == null) {
	throw new IllegalStateException("mongod service not found. mongod must be installed before mongos.");
}
mongodHostInstances = mongodService.waitForInstances(mongodService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 

println "mongos_poststart.groovy: mongodHostInstances length is "+mongodHostInstances.length

currPort = serviceContext.attributes.thisInstance["port"] as int
println "mongos_poststart.groovy: Connecting to mongos on port ${currPort} ..."

mongo = new GMongo("127.0.0.1", currPort)
	
println "mongos_poststart.groovy: After new GMongo port ${currPort} ..."
	
db = mongo.getDB("admin")
assert db != null 	
println "Connection succeeded"	
	
	
mongodInstances=serviceContext.attributes.mongod.instances		
	
mongodHostInstances.each {
		mongodPort = mongodInstances[it.instanceID].port		
		mongodHost = it.hostAddress		
		println "mongos_poststart.groovy: mongod #"+it.instanceID + " host and port = ${mongodHost}:${mongodPort}"		
		result = db.command(["addshard":"${mongodHost}:${mongodPort}"])		
		println "mongos_poststart.groovy: db result: ${result}"
}
    
	
result = db.command(["enablesharding":"petclinic"])	
println "mongos_poststart.groovy: db result: ${result}"
	
result = db.command(["shardcollection":"petclinic.Person", "key":["_id":1]])
println "mongos_poststart.groovy: db result: ${result}"
	

 


