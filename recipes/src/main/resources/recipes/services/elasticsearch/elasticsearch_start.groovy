import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("elasticsearch.properties").toURL())

new AntBuilder().sequential {
	exec(executable:config.script, osfamily:"unix"){
		arg value:"-f"
	}
	exec(executable:"${config.script}.bat", osfamily:"windows")
}
