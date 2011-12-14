import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.j_spaces.kernel.Environment
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.InetAddress

admin = ServiceContextFactory.getServiceContext().getAdmin()
admin.getProcessingUnits().waitFor("rest", 60, TimeUnit.SECONDS).waitFor(1, 60, TimeUnit.SECONDS)
restMachine = admin.getProcessingUnits().getProcessingUnit("rest").getInstances()[0].getMachine().getHostAddress()
restUrl = "http://${restMachine}:8100/rest/"

cliPath = new File(Environment.getHomeDirectory(), "\\tools\\cli\\cloudify.bat").toString()

removeRewriteLbCommand = [
	"name" : "travel"
]

paramsMap = removeRewriteLbCommand.collect { k, v -> "'${k}=${v}'" }.join(' ')
println("${cliPath} connect ${restUrl}; use-application Management; invoke iisproxy rewrite_remove_external_lb [ ${paramsMap} ]".execute().text)

