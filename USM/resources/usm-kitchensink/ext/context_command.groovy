import java.util.concurrent.TimeUnit

println "context_command"
//if (args && args[0].toFloat() > 0) numTweets = args[0]

def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
println "context is:  " + context
def serviceName = context.serviceName;
println "Service name is: " + serviceName
def service = context.waitForService(serviceName, 20, TimeUnit.SECONDS)
if(service == null) {
	throw new Exception("Could not find service: " + serviceName)
}
println "service is: " + service

def instance = service.waitForInstances(1, 10, TimeUnit.SECONDS)
if(instance == null) {
	throw new Exception("Could not find instance of service in context")
}

println "instance is: " + instance


