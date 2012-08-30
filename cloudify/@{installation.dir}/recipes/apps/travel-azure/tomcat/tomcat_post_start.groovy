import org.cloudifysource.dsl.context.ServiceContextFactory
import com.j_spaces.kernel.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.InetAddress

//load configuraiton
def serviceContext = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${serviceContext.serviceName}-service.properties").toURL())

// discover rest server URL
admin = ServiceContextFactory.getServiceContext().getAdmin()
admin.getProcessingUnits().waitFor("rest", 60, TimeUnit.SECONDS).waitFor(1, 60, TimeUnit.SECONDS)
restMachine = admin.getProcessingUnits().getProcessingUnit("rest").getInstances()[0].getMachine().getHostAddress();
restUrl = "http://${restMachine}:8100/rest/"

//run cloudify invoke command that updates the load balancer
cliPath = new File(Environment.getHomeDirectory(), "\\tools\\cli\\cloudify.bat").toString()

def process = "cmd /c call ${cliPath} connect ${restUrl}; use-application management; invoke iisproxy rewrite_add_external_lb ${config.appFolder} 8080".execute();
process.waitForProcessOutput(System.out, System.err)
if (process.exitValue() != 0) {
	throw new Exception("Failed to set iisproxy rewrite rule");
}


