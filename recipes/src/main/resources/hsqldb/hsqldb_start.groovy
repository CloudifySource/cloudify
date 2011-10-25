import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("hsqldb.properties").toURL())

new AntBuilder().sequential {
	java(classname:"org.hsqldb.server.Server", 
		classpath:"${config.installDir}/hsqldb-${config.version}/hsqldb/lib/hsqldb.jar",
		dir:"${config.installDir}/hsqldb-${config.version}/hsqldb/data", fork:true) {
		jvmarg value:"-Dcom.sun.management.jmxremote"
		jvmarg value:"-Dcom.sun.management.jmxremote.port=${config.jmxPort}"
		jvmarg value:"-Dcom.sun.management.jmxremote.ssl=false"
		jvmarg value:"-Dcom.sun.management.jmxremote.authenticate=false"
	}
}
