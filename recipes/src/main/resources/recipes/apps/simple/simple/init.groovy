import java.util.concurrent.TimeUnit

println "Classpath is: " + System.getenv().get("CLASSPATH");
println "This is the init event"
def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
def service = context.waitForService(context.clusterInfo.name, 20, TimeUnit.SECONDS)

println "Context: " + context
println "Context Service: " + service
println "Service Name: " + service.name
println "Admin: " + context.admin
println "Service Planned Instances: " + service.numberOfPlannedInstances
println "Service Actual Instances: " + service.numberOfActualInstances
if (service.numberOfActualInstances > 0) {
    service.instances.each {
        println "Service Instance ID: " + it.instanceID
        println "Service Instance Host Address: " + it.hostAddress
        println "Service Instance Host Name: " + it.hostName
    }
}