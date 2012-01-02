import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.j_spaces.kernel.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.InetAddress

// discover rest server URL
admin = ServiceContextFactory.getServiceContext().getAdmin()
admin.getProcessingUnits().waitFor("management.rest", 60, TimeUnit.SECONDS).waitFor(1, 60, TimeUnit.SECONDS)
restMachine = admin.getProcessingUnits().getProcessingUnit("management.rest").getInstances()[0].getMachine().getHostAddress();
restUrl = "http://${restMachine}:8100/rest/"

//run cloudify invoke command that updates the load balancer
cliPath = new File(Environment.getHomeDirectory(), "\\tools\\cli\\cloudify.bat").toString()

rewriteLbCommand = [
	"name" : "travel",
	"port" : 8080
]

paramsMap = rewriteLbCommand.collect { k, v -> "'${k}=${v}'" }.join(' ')
def process = "cmd /c call ${cliPath} connect ${restUrl}; use-application management; invoke iisproxy rewrite_add_external_lb ${paramsMap}".execute();
process.waitForProcessOutput(System.out, System.err)
if (process.exitValue() != 0) {
	throw new Exception("Failed to set iisproxy rewrite rule");
}


