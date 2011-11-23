import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.j_spaces.kernel.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.InetAddress

admin = ServiceContextFactory.getServiceContext().getAdmin()
restMachine = admin.getProcessingUnits().waitFor("rest", 60, TimeUnit.SECONDS).getInstances()[0].getMachine().getHostAddress()
restUrl = "http://${restMachine}:8100/rest/"

cliPath = new File(Environment.getHomeDirectory(), "\\tools\\cli\\cloudify.bat").toString()

// assuming a single tomcat instance
thisHost = InetAddress.localHost.hostAddress

rewriteCommand = [
	"name" : "travel",
	"patternSyntax" : "ECMAScript",
	"pattern" : "^travel/(.*)",
	"rewriteUrl" : "http://${thisHost}:8080/travel/{R:1}",
	"stopProcessing" : "true" 
]

outbountRewriteCommand = [
	"name" : "travel_outbound",
	"conditionPattern" : "^/travel/.*",
	"rewriteUrl" : "/travel/{R:1}"
]

paramsMap = rewriteCommand.collect { k, v -> "'${k}=${v}'" }.join(' ')
println("${cliPath} connect ${restUrl}; use-application Management; invoke iisproxy rewrite_add [ ${paramsMap} ]".execute().text)

// doesn't work on azure (don't know why)
// paramsMap = outbountRewriteCommand.collect { k, v -> "'${k}=${v}'" }.join(' ')
// println("${cliPath} connect ${restUrl}; use-application Management; invoke iisproxy rewrite_outbound_add [ ${paramsMap} ]".execute().text)

