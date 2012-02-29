@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo

config = new ConfigSlurper().parse(new File('mongoConfig.properties').toURL())

println "sleeping for 5 secs"

sleep(5000)

println "Checking connection to mongo on port ${config.port}"
try {
    //check connection 
	mongo = new GMongo("127.0.0.1", config.port)
	db = mongo.getDB("admin")
	assert db != null 
    println "Connection succeeded"
	mongo.close()
} catch (Exception e) {
    println "Connection Failed!"
	throw e; 
}
