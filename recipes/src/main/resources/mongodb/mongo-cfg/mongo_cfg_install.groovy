import org.cloudifysource.dsl.utils.ServiceUtils;


config = new ConfigSlurper().parse(new File("mongo-cfg.properties").toURL())
osConfig = ServiceUtils.isWindows() ? config.win32 : config.linux

builder = new AntBuilder()
builder.sequential {
	mkdir(dir:config.installDir)
	get(src:osConfig.downloadPath, dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(ServiceUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:config.home)
