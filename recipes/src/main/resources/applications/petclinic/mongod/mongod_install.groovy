import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.usm.USMUtils

serviceContext = ServiceContextFactory.getServiceContext()

config = new ConfigSlurper().parse(new File("mongod.properties").toURL())
osConfig = USMUtils.isWindows() ? config.win32 : config.unix



instanceID = serviceContext.getInstanceId()

installDir = System.properties["user.home"]+ "/.cloudify/${config.service}" + instanceID


home = "${serviceContext.serviceDirectory}/mongodb-${config.version}"
//home = "${installDir}/mongodb-${config.version}"

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

if(USMUtils.isWindows()) {
	builder.unzip(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", overwrite:true)
} else {
	builder.untar(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", compression:"gzip", overwrite:true)
	builder.chmod(dir:"${installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}

println "mongod_install.groovy: mongod(${instanceID}) moving ${installDir}/${osConfig.name} to ${home}..."
builder.move(file:"${installDir}/${osConfig.name}", tofile:"${home}")

println "mongod_install.groovy: mongod(${instanceID}) ended"