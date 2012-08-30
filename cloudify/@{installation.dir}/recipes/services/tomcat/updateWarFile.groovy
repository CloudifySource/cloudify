import org.cloudifysource.dsl.context.ServiceContextFactory

println "updateWarFile.groovy: Starting..."

context = ServiceContextFactory.getServiceContext()
config  = new ConfigSlurper().parse(new File("${context.serviceDirectory}/tomcat-service.properties").toURL())

def instanceID = context.getInstanceId()
installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID
applicationWar = "${installDir}/${config.warName}"

newWarFile=context.attributes.thisService["warUrl"] 
println "updateWarFile.groovy: newWarFile is ${newWarFile}"

home = context.attributes.thisInstance["home"]
webApps="${home}/webapps"
destWarFile="${webApps}/${config.warName}"
println "updateWarFile.groovy: destWarFile is ${destWarFile}"

new AntBuilder().sequential {
	
	echo(message:"updateWarFile.groovy: Getting ${newWarFile} ...")
	get(src:"${newWarFile}", dest:"${applicationWar}", skipexisting:false)
	
	echo(message:"updateWarFile.groovy: Copying ${applicationWar} to ${webApps}...")
	copy(todir: "${webApps}", file:"${applicationWar}", overwrite:true)	
}

println "updateWarFile.groovy: End"
return true


