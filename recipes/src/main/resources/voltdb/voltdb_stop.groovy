config = new ConfigSlurper().parse(new File("voltdb-service.properties").toURL())

new AntBuilder().sequential {
	exec(executable:config.script, osfamily:"unix") {
		arg value:"stop"
	}
}
