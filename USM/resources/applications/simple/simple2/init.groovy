import org.cloudifysource.dsl.context.ServiceContext
import org.cloudifysource.dsl.context.ServiceContextFactory;


println "Classpath is: " + System.getenv().get("CLASSPATH");
println "This is the init event"

ServiceContext context = ServiceContextFactory.getServiceContext()

println "Context: " + context
println "Context Service: " + context.service
println "Service Name: " + context.service.name
println "Admin: " + context.admin
println "Service Planned Instances: " + context.service.numberOfPlannedInstances
println "Service Actual Instances: " + context.service.numberOfActualInstances
if(context.service.numberOfActualInstances > 0) {
	context.service.instances.each {
		println "Service Instance ID: " + it.instanceId
		println "Service Instance Host Address: " + it.hostAddress
		println "Service Instance Host Name: " + it.hostName
	}
}