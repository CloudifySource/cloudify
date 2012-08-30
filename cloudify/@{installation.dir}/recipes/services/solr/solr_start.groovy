import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("solr.properties").toURL())

new AntBuilder().sequential {
	java(jar:"${config.home}/example/start.jar", 
		dir:"${config.home}/example", fork:true) {
		jvmarg value:"-Dcom.sun.management.jmxremote"
		jvmarg value:"-Dcom.sun.management.jmxremote.port=${config.jmxPort}"
		jvmarg value:"-Dcom.sun.management.jmxremote.ssl=false"
		jvmarg value:"-Dcom.sun.management.jmxremote.authenticate=false"
	}
}
