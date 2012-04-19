config = new ConfigSlurper().parse(new File('mongo-cfg.properties').toURL())

//start mongod
dataDir = "${config.home}/data/cfg"

new AntBuilder().sequential {
	//creating the data directory 	
    delete(file:"${dataDir}/mongod.lock")
	mkdir(dir:dataDir)
	exec(executable:config.script) {
		arg value:"--configsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${config.port}"
    }
}

