config = new ConfigSlurper().parse(new File('mongod.properties').toURL())

dataDir = "${config.home}/data"

new AntBuilder().sequential {
	//creating the data directory 	
	mkdir(dir:"${dataDir}")
    delete(file:"${dataDir}/mongod.lock")
	exec(executable:config.script) {
		arg line:"--shardsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${config.port}"
	}
}
