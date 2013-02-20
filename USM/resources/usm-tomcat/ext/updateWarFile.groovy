import org.cloudifysource.dsl.context.ServiceContextFactory

println "updateWarFile.groovy: Starting..."

context = ServiceContextFactory.getServiceContext()
config  = new ConfigSlurper().parse(new File("${context.serviceDirectory}/tomcat-service.properties").toURL())

newWarFile=context.attributes.thisService["warUrl"] 
println "updateWarFile.groovy: newWarFile is ${newWarFile}"

home = context.attributes.thisInstance["home"]
webApps="${home}/webapps"
destWarFile="${webApps}/${config.warName}"
println "updateWarFile.groovy: destWarFile is ${destWarFile}"

new AntBuilder().sequential {
	
	echo(message:"updateWarFile.groovy: Getting ${newWarFile} ...")
	get(src:"${newWarFile}", dest:"${config.applicationWar}", skipexisting:false)
	
	echo(message:"updateWarFile.groovy: Copying ${config.applicationWar} to ${webApps}...")
	copy(todir: "${webApps}", file:"${config.applicationWar}", overwrite:true)	
}

println "updateWarFile.groovy: End"
return true


