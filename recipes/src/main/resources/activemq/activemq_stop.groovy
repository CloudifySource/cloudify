config = new ConfigSlurper().parse(new File("activemq.properties").toURL())

new AntBuilder().sequential {
	exec(executable:config.script, osfamily:"unix") {
		arg value:"stop"
	}
	exec(executable:"${config.script}.bat", osfamily:"windows"){
		arg value: "stop"
	}
}
