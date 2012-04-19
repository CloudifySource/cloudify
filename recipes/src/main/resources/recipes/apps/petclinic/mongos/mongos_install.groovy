import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils;


config = new ConfigSlurper().parse(new File("mongos-service.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.unix

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()

println "mongos_install.groovy: Writing mongos port to this instance(${instanceID}) attributes..."
serviceContext.attributes.thisInstance["port"] = config.basePort+instanceID
port = serviceContext.attributes.thisInstance["port"] 
println "mongos_install.groovy: mongos(${instanceID}) port ${port}"

home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"
script = "${home}/bin/mongos"

serviceContext.attributes.thisInstance["home"] = "${home}"
serviceContext.attributes.thisInstance["script"] = "${script}"
println "mongos_install.groovy: mongos(${instanceID}) home is ${home}"


builder = new AntBuilder()
builder.sequential {
	mkdir(dir:"${config.installDir}")
	get(src:"${osConfig.downloadPath}", dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(ServiceUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:"${config.installDir}", overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:"${config.installDir}", compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:"${home}")

println "mongos_install.groovy: Installation of mongos(${instanceID}) ended"