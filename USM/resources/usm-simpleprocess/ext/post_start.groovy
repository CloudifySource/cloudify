import java.util.concurrent.TimeUnit

println "postStart fired"

if (args && args[0].toFloat() > 0) numTweets = args[0]

def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
println "context is:  " + context
def puName = context.clusterInfo.name;
println "PU name is: " + puName 
println "Service name is: " + context.serviceName
println "Application name is: " + context.applicationName

def service = context.waitForService(context.serviceName, 20, TimeUnit.SECONDS)
if(service == null) {
	throw new Exception("Could not find service: " + puName)
}
println "service is: " + service

//def instance = service.waitForInstances(1, 10, TimeUnit.SECONDS)
//if(instance == null) {
//	throw new Exception("Could not find instance of service in context")
//}
//
//println "instance is: " + instance


