config = new ConfigSlurper().parse(new File("redis-service.properties").toURL())

new AntBuilder().sequential {
	exec(executable:config.script, osfamily:"unix")
}
