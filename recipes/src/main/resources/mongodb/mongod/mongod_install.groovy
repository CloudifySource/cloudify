import com.gigaspaces.cloudify.dsl.context.ServiceContextFactory
import com.gigaspaces.cloudify.usm.USMUtils

println("Checking operating system's compatibility with mongod")
if (!USMUtils.isWindows()){
	String command = "uname -a"
	Process pr = command.execute()
	pr.waitFor();
	if (pr.in.text.contains("2007")){
		throw new Exception("The Linux legacy-static builds are needed for older systems." 
			+ "Try and run mongod from the commandline and if you get a floating point exception, use a legacy-static build.");  
	}
}
println("op system is compatible with mongod")

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
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:config.home)
