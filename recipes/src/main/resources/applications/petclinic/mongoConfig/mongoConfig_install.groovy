import org.cloudifysource.dsl.context.ServiceContextFactory


def config = new ConfigSlurper().parse(new File("mongoConfig.properties").toURL())
def osConfig = ServiceUtils.isWindows() ? config.win32 : config.unix

def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()
println "mongoConfig_install.groovy: Writing mongoConfig port to this instance(${instanceID}) attributes..."

def home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"
def script = "${home}/bin/mongod"

serviceContext.attributes.thisInstance["home"] = "${home}"
serviceContext.attributes.thisInstance["script"] = "${script}"
println "mongoConfig_install.groovy: mongoConfig(${instanceID}) home is ${home}"


serviceContext.attributes.thisInstance["port"]=config.basePort+instanceID
def port=serviceContext.attributes.thisInstance["port"]
println "mongoConfig_install.groovy: mongoConfig(${instanceID}) is using port ${port}"

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

println "mongoConfig_install.groovy: Installation of mongoConfig(${instanceID}) ended"