config = new ConfigSlurper().parse(new File("redis-service.properties").toURL())

new AntBuilder().sequential {
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	untar(src:"${config.installDir}/${config.zipName}", dest:config.installDir, compression:"gzip")
	exec(executable:"make", dir:"${config.installDir}/${config.name}", osfamily:"unix")
}	
