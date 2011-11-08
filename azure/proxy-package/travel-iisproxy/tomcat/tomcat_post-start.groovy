import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit
import java.net.InetAddress

serviceContext = ServiceContextFactory.getServiceContext()
admin = serviceContext.getAdmin()

pu = admin.getProcessingUnits().waitFor("iisproxy", 60, TimeUnit.SECONDS);
pu.waitFor(1, 60, TimeUnit.SECONDS)
instance = pu.getInstances()[0]

// assuming a single tomcat instance
thisHost = InetAddress.localHost.hostAddress

rewriteCommand = [
	"GS_USM_CommandName" : "rewrite_add",
	"name" : "travel",
	"patternSyntax" : "ECMAScript",
	"pattern" : "^travel/(.*)",
	"rewriteUrl" : "http://${thisHost}:8080/travel/{R:1}",
	"stopProcessing" : "true" 
]

outbountRewriteCommand = [
	"GS_USM_CommandName" : "rewrite_outbound_add",
	"name" : "travel_outbound",
	"conditionPattern" : "^/travel/.*",
	"rewriteUrl" : "/travel/{R:1}"
]

instance.invoke("universalServiceManagerBean", rewriteCommand).get(60, TimeUnit.SECONDS);

// doesn't work on azure (don't know why)
// instance.invoke("universalServiceManagerBean", outbountRewriteCommand).get(60, TimeUnit.SECONDS);
