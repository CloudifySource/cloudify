import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.gigaspaces.cloudify.usm.USMUtils

serviceContext = ServiceContextFactory.getServiceContext()
instanceIdFile = new File("./instanceId.txt")
instanceIdFile.text = serviceContext.instanceId

config = new ConfigSlurper().parse(new File("mongod.properties").toURL())
osConfig = USMUtils.isWindows() ? config.win32 : config.linux

portFile = new File("./port.txt")
portFile.text = config.port

builder = new AntBuilder()
builder.sequential {
	mkdir(dir:config.installDir)
	get(src:osConfig.downloadPath, dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(USMUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
File configHome = new File(config.home);
if (configHome.exists()){
	println("mongoD home foler exists");
}else{
	println("mongoD home foler exists");
}
if (!configHome.exists() || configHome.listFiles().length == 0){
	builder.move(file:"${config.installDir}/${osConfig.name}", tofile:config.home)
	println("Moved mongoD files successfully")
}
