import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())

script = "${config.home}/bin/cassandra-cli"
new AntBuilder().sequential {
	exec(executable:script, osfamily:"unix") {
		arg value:"-host"
		arg value:"localhost"
		arg value:"-port"
		arg value:"9160"
		arg value:"-f"
		arg value:"cassandraSchema.txt"
	}
	exec(executable:"${script}.bat", osfamily:"windows") {
		arg value:"-host"
		arg value:"localhost"
		arg value:"-port"
		arg value:"9160"
		arg value:"-f"
		arg value:"cassandraSchema.txt"
	}
	echo(message:"created cassandra schema")
}
