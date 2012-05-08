config = new ConfigSlurper().parse(new File("couchdb-service.properties").toURL())

new AntBuilder().sequential {
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	untar(src:"${config.installDir}/${config.zipName}", dest:config.installDir, compression:"gzip")
}	
