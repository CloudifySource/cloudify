config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

//start tomcat
println "executing command ${config.script}"
new AntBuilder().sequential {
	exec(executable:"${config.script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
		arg(value:"run")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows") {
        env(key:"CATALINA_HOME", value: "${config.home}")
		env(key:"CATALINA_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")		
		arg(value:"run")
	}
}
