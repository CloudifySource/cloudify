config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

println "executing command ${config.script}"
new AntBuilder().sequential {
	exec(executable:"${config.script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${config.home}")
    env(key:"CATALINA_BASE", value: "${config.home}")
    env(key:"CATALINA_TMPDIR", value: "${config.home}/temp")
		arg(value:"stop")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows"){
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_BASE", value: "${config.home}")
    env(key:"CATALINA_TMPDIR", value: "${config.home}/temp")
		arg(value:"stop")
	}
}