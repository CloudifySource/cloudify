import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils;


config = new ConfigSlurper().parse(new File("mongos-service.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.unix

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()

installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID

println "mongos_install.groovy: Writing mongos port to this instance(${instanceID}) attributes..."
serviceContext.attributes.thisInstance["port"] = config.port



home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"
script = "${home}/bin/mongos"

serviceContext.attributes.thisInstance["home"] = "${home}"
serviceContext.attributes.thisInstance["script"] = "${script}"
println "mongos_install.groovy: mongos(${instanceID}) home is ${home}"


builder = new AntBuilder()
builder.sequential {
	mkdir(dir:"${installDir}")
	get(src:"${osConfig.downloadPath}", dest:"${installDir}/${osConfig.zipName}", skipexisting:true)
}

if(ServiceUtils.isWindows()) {
	builder.unzip(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", overwrite:true)
} else {
	builder.untar(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", compression:"gzip", overwrite:true)
}
builder.move(file:"${installDir}/${osConfig.name}", tofile:"${home}")

if(!ServiceUtils.isWindows()) {
	println "calling chmod on ${home}/bin"
	builder.chmod(dir:"${home}/bin", perm:'+x', includes:"*")
}

println "mongos_install.groovy: Installation of mongos(${instanceID}) ended"