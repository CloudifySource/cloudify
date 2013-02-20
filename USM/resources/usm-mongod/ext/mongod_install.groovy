import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.context.ServiceContextFactory


serviceContext = ServiceContextFactory.getServiceContext()

config = new ConfigSlurper().parse(new File("mongod-service.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.unix



instanceID = serviceContext.getInstanceId()

installDir = "${serviceContext.serviceDirectory}/${config.serviceName}" + instanceID


home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"


// serviceContext.attributes.thisInstance["home"] = "${home}"
println "mongod_install.groovy: mongod(${instanceID}) home is ${home}"

// serviceContext.attributes.thisInstance["script"] = "${home}/bin/mongod"
println "mongod_install.groovy: mongod(${instanceID}) script is ${home}/bin/mongod"

currPort=config.port
if (serviceContext.isLocalCloud()) {
	if (config.sharded) {
		currPort+=instanceID-1
	}
}

//serviceContext.attributes.thisInstance["port"] = currPort

println "mongod_install.groovy: mongod(${instanceID}) port ${currPort}"


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

println "mongod_install.groovy: mongod(${instanceID}) moving ${installDir}/${osConfig.name} to ${home}..."
builder.move(file:"${installDir}/${osConfig.name}", tofile:"${home}")

if(!ServiceUtils.isWindows()) {
	println "calling chmod on ${home}/bin"
	builder.chmod(dir:"${home}/bin", perm:'+x', includes:"*")
}

println "mongod_install.groovy: mongod(${instanceID}) ended"