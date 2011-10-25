config = new ConfigSlurper().parse(new File("nginx.properties").toURL())
new AntBuilder().exec(executable:config.script, dir:config.home){
	arg line:"-s quit"
}
