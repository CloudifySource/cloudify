import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.j_spaces.kernel.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.InetAddress

admin = ServiceContextFactory.getServiceContext().getAdmin()
restMachine = admin.getProcessingUnits().waitFor("rest", 60, TimeUnit.SECONDS).getInstances()[0].getMachine().getHostAddress()
restUrl = "http://${restMachine}:8100/rest/"

cliPath = new File(Environment.getHomeDirectory(), "\\tools\\cli\\cloudify.bat").toString()

rewriteLbCommand = [
	"name" : "travel",
	"port" : 8080
]

paramsMap = rewriteLbCommand.collect { k, v -> "'${k}=${v}'" }.join(' ')
println("${cliPath} connect ${restUrl}; use-application Management; invoke iisproxy rewrite_add_external_lb [ ${paramsMap} ]".execute().text)


