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

removeRewriteCommand = [
	"GS_USM_CommandName" : "rewrite_remove",
	"name" : "travel",
	"patternSyntax" : "ECMAScript",
	"pattern" : "^travel/(.*)",
	"rewriteUrl" : "http://${thisHost}:8080/travel/{R:1}",
	"stopProcessing" : "true" 
]

removeOutbountRewriteCommand = [
	"GS_USM_CommandName" : "rewrite_outbound_remove",
	"name" : "travel_outbound"
]

instance.invoke("universalServiceManagerBean", removeRewriteCommand).get(60, TimeUnit.SECONDS);

// doesn't work on azure (don't know why)
// instance.invoke("universalServiceManagerBean", removeOutbountRewriteCommand).get(60, TimeUnit.SECONDS);
