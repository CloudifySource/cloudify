config = new ConfigSlurper().parse(new File("jboss.properties").toURL())

script = "${config.home}/bin/standalone"
new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix")
	exec(executable:"${script}.bat", osfamily:"windows")
}