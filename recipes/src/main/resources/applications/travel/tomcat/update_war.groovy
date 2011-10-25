config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

new AntBuilder().sequential {
	get(src:config.applicationWarUrl, dest:config.applicationWar, skipexisting:false)
	copy(todir: "${config.home}/webapps",  file:config.applicationWar, overwrite:true)
}
