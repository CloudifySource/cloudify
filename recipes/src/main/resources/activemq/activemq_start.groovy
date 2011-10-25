config = new ConfigSlurper().parse(new File("activemq.properties").toURL())

new AntBuilder().sequential {
	exec(executable:config.script, osfamily:"unix") {
		arg(value:"console")
		env(key:"ACTIVEMQ_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
	}
	exec(executable:"${config.script}.bat", osfamily:"windows"){
		env(key:"ACTIVEMQ_OPTS", value:"-Dcom.sun.management.jmxremote.port=11099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
	}
}
