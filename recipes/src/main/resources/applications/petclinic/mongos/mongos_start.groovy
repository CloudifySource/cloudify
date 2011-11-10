import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File('mongos.properties').toURL())

serviceContext = ServiceContextFactory.getServiceContext()
mongoService = serviceContext.waitForService("petclinic-mongo.mongo-cfg", 20, TimeUnit.SECONDS)
mongoInstances = mongoService.waitForInstances(mongoService.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
cfgHost = mongoInstances[0].hostAddress

new AntBuilder().exec(executable:config.script) {
	arg line:"--configdb ${cfgHost}:${config.cfgPort}"
	arg line:"--port ${config.port}"
	arg line:"--chunkSize 1"
}
