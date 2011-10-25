@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo

import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos.properties').toURL())

println "sleeping for 10 secs"

serviceContext = ServiceContextFactory.getServiceContext()

//waiting for mongod service to become available, will not be needed in one of the upcoming builds 
println "waiting for mongod"
mongodService = serviceContext.waitForService("mongod", 20, TimeUnit.SECONDS) 
mongodHostInstances = mongodService.waitForInstances(mongodService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 

println "Connecting to mongos on port ${config.port}"
try {
    //check connection 
	mongo = new GMongo("127.0.0.1", config.port)
	db = mongo.getDB("admin")
	assert db != null 	
    println "Connection succeeded"	

	mongodHostInstances.each {
		mongodPort = config.mongodBasePort + it.instanceID
		mongodHost = it.hostAddress		
		println "mongod #"+it.instanceID + " host and port = ${mongodHost}:${mongodPort}"		
		result = db.command(["addshard":"${mongodHost}:${mongodPort}"])		
		println result
	}	
    
	result = db.command(["enablesharding":"petclinic"])	
	println result 	
	
	result = db.command(["shardcollection":"petclinic.Person", "key":["_id":1]])
	println result 	
} catch (Exception e) {
    println "Connection Failed!"
	throw e; 
}

 


