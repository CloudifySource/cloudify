import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("jboss.properties").toURL())

script = "${config.home}/bin/jboss-admin"
new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix"){
		arg value:"--connect"
		arg value:"command=:shutdown"
	}
	exec(executable:"${script}.bat", osfamily:"windows"){
		arg value:"--connect"
		arg value:"command=:shutdown"
	}
}