import org.cloudifysource.dsl.context.ServiceContext
import org.cloudifysource.usm.USMUtils

println "Classpath is: " + System.getenv().get("CLASSPATH");
println "This is the init event"
ServiceContext context = USMUtils.getServiceContext ()
USMUtils.invokeMethod "getServiceContext", null

//ServiceContext ctx =org.cloudifysource.usm.USMUtils.getServiceContext
println "Context: " + context
println "Context Service: " + context.service
println "Service Name: " + context.service.name
println "Admin: " + context.admin
println "Service Planned Instances: " + context.service.numberOfPlannedInstances
println "Service Actual Instances: " + context.service.numberOfActualInstances
if(context.service.numberOfActualInstances > 0) {
	context.service.instances.each {
		println "Service Instance ID: " + it.instanceID
		println "Service Instance Host Address: " + it.hostAddress
		println "Service Instance Host Name: " + it.hostName
	}
}