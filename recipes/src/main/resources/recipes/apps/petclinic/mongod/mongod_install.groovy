import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.context.ServiceContextFactory


serviceContext = ServiceContextFactory.getServiceContext()

config = new ConfigSlurper().parse(new File("mongod-service.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.unix



instanceID = serviceContext.getInstanceId()

installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID


home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"


serviceContext.attributes.thisInstance["home"] = "${home}"
println "mongod_install.groovy: mongod(${instanceID}) home is ${home}"

serviceContext.attributes.thisInstance["script"] = "${home}/bin/mongod"
println "mongod_install.groovy: mongod(${instanceID}) script is ${home}/bin/mongod"

serviceContext.attributes.thisInstance["port"] = config.basePort+instanceID

def port = serviceContext.attributes.thisInstance["port"] 
intPort=port.intValue()
println "mongod_install.groovy: mongod(${instanceID}) port ${intPort}"


builder = new AntBuilder()
builder.sequential {
	mkdir(dir:"${installDir}")
	get(src:"${osConfig.downloadPath}", dest:"${installDir}/${osConfig.zipName}", skipexisting:true)
}

if(ServiceUtils.isWindows()) {
	builder.unzip(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", overwrite:true)
} else {
	builder.untar(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", compression:"gzip", overwrite:true)
	builder.chmod(dir:"${installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}

println "mongod_install.groovy: mongod(${instanceID}) moving ${installDir}/${osConfig.name} to ${home}..."
builder.move(file:"${installDir}/${osConfig.name}", tofile:"${home}")

println "mongod_install.groovy: mongod(${instanceID}) ended"